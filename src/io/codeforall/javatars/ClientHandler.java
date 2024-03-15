package io.codeforall.javatars;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private Socket socket;
    private QuizServer server;
    private PrintWriter out;
    private int score = 0;
    private String nickname = "";

    // Constructor to initialize fields, including server reference
    public ClientHandler(Socket socket, QuizServer server) {
        this.socket = socket;
        this.server = server;
        // Initialize other fields...
    }

    @Override
    public void run() {



        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            out = new PrintWriter(socket.getOutputStream(), true);
            out.println("Enter your nickname:");
            nickname = reader.readLine();
            server.broadcastMessage(nickname + " has joined the server!");
            String inputLine;
            while ((inputLine = reader.readLine()) != null) {
                handleCommand(inputLine);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // Handle disconnection
            //server.removeClient(this);
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleCommand(String input) {
        if (input.equalsIgnoreCase("/join")) {
            server.registerPlayer(this);
        }
        if (server.isGameStarted()){
            server.answerQuestion(this, input);
        }
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getScore() {
        return score;
    }

    public String getNickname() {
        return nickname;
    }
}
