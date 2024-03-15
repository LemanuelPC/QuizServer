package io.codeforall.javatars.Questions;

public class OpenEndedQuestion extends Question {

    private String correctAnswer;

    public OpenEndedQuestion(String questionText, String correctAnswer) {
        super(questionText);
        this.correctAnswer = correctAnswer;
    }

    @Override
    public boolean checkAnswer(String answer) {
        return answer.trim().equalsIgnoreCase(correctAnswer);
    }

    @Override
    public String formatQuestion() {
        return questionText + "\n(Write your answer below)";
    }

    @Override
    public String getAnswer() {
        return correctAnswer;
    }

}
