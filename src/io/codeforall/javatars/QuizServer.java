package io.codeforall.javatars;

import io.codeforall.javatars.Questions.MultipleChoiceQuestion;
import io.codeforall.javatars.Questions.OpenEndedQuestion;
import io.codeforall.javatars.Questions.Question;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class QuizServer {

    private ServerSocket serverSocket;
    private ExecutorService executorService;
    private final Set<ClientHandler> playerHandlers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<ClientHandler> watcherHandlers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private volatile boolean gameStarted = false;
    private List<Question> questions; // Assuming this is filled elsewhere
    private Iterator<Question> questionIterator;
    private Map<ClientHandler, String> answers = new ConcurrentHashMap<>();
    private TimerTask currentQuestionTask = null;

    public QuizServer(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        executorService = Executors.newCachedThreadPool();
        questions = new LinkedList<>();
        startConsoleCommandListener();
    }

    public synchronized void registerPlayer(ClientHandler player) {
        if (!gameStarted) {
            playerHandlers.add(player);
            player.sendMessage("\nYou've joined the game. Wait for it to start.\n");
        } else {
            watcherHandlers.add(player);
            player.sendMessage("\nGame has already started, you're now watching.\n");
        }
    }

    private void startConsoleCommandListener() {
        // Use a separate thread for listening to console commands to not block the main thread
        new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            while (true) {
                String command = scanner.nextLine();
                if ("/start".equalsIgnoreCase(command)) {
                    startGame();
                }
                // You can add more commands here as needed
            }
        }).start();
    }

    private void loadQuizQuestions() {
        // Load or define quiz questions here
        questions.add(new MultipleChoiceQuestion("What is 2+2?\n", Arrays.asList("2", "3", "4", "5"), "4"));
        questions.add(new OpenEndedQuestion("Name the largest ocean on Earth.\n", "Pacific Ocean"));
    }



    public void startServer() {
        loadQuizQuestions();
        while (true) {
            try {
                // Accept new client connection and submit it to the executor service
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket, this);
                executorService.submit(handler); // Use executor service to handle client
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws IOException {
        QuizServer server = new QuizServer(8080); // Use your desired port
        server.startServer();
    }

    public synchronized void startGame() {
        if (!gameStarted && !playerHandlers.isEmpty()) {
            gameStarted = true;
            questionIterator = questions.iterator();
            broadcastMessage("\nGame is starting!\n");
            nextQuestion();
        }
    }

    private synchronized void nextQuestion() {
        if (questionIterator.hasNext()) {
            Question question = questionIterator.next();
            broadcastMessage(question.formatQuestion());
            if (currentQuestionTask != null) {
                currentQuestionTask.cancel();
            }
            Timer timer = new Timer();
            currentQuestionTask = new TimerTask() {
                @Override
                public void run() {
                    synchronized (QuizServer.this) {
                        evaluateAnswers(question);
                        nextQuestion();
                    }
                }
            };
            timer.schedule(currentQuestionTask, 20000); // Schedule new task
        } else {
            endGame();
        }
    }

    private synchronized void evaluateAnswers(Question question) {
        // Evaluation logic remains the same
        answers.forEach((clientHandler, answer) -> {
            boolean isCorrect = question.checkAnswer(answer);
            if (isCorrect) {
                clientHandler.setScore(clientHandler.getScore() + question.getScore()); // Assume each question scores 1 point
                clientHandler.sendMessage("Correct answer!\n");
            } else {
                clientHandler.sendMessage("Wrong answer! The correct answer was: " + question.getAnswer() + "\n");
            }
        });
        answers.clear(); // Ready for next question
        // It might be a good idea to move `nextQuestion()` call here if you want immediate transition
        evaluateAnswers(questionIterator.next()); // Evaluate immediately if all answers are received
        nextQuestion(); // Move to the next question
    }

    public synchronized void answerQuestion(ClientHandler client, String answer) {
        if (gameStarted && playerHandlers.contains(client)) {
            answers.put(client, answer);
            if (answers.size() == playerHandlers.size()) {
                // Cancel the current timer task as all answers are received
                if (currentQuestionTask != null) {
                    currentQuestionTask.cancel();
                    currentQuestionTask = null;
                }
            }
        }
    }

    private void endGame() {
        StringBuilder finalScores = new StringBuilder("\nGame over!\nFinal scores:\n");
        playerHandlers.stream()
                .sorted((a, b) -> b.getScore() - a.getScore()) // Sort by score in descending order
                .forEach(player -> finalScores.append(player.getNickname()).append(": ").append(player.getScore()).append("\n"));

        broadcastMessage(finalScores.toString());


        stopServer();
    }

    // Method to broadcast messages to players and watchers
    public void broadcastMessage(String message) {
        playerHandlers.forEach(handler -> handler.sendMessage(message));
        watcherHandlers.forEach(handler -> handler.sendMessage(message));
    }

    public void stopServer() {
        try {
            serverSocket.close();
            executorService.shutdown(); // Shutdown the executor service
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (IOException | InterruptedException e) {
            executorService.shutdownNow();
        }
    }

    public boolean isGameStarted() {
        return gameStarted;
    }
}
