package io.codeforall.javatars.Questions;

public abstract class Question {
    protected String questionText;
    protected int score;

    public Question(String questionText) {
        this.questionText = questionText;
    }

    public String getQuestionText() {
        return questionText;
    }

    public abstract boolean checkAnswer(String answer);
    public abstract String formatQuestion();
    public abstract String getAnswer();

    public int getScore() {
        return score;
    }
}

