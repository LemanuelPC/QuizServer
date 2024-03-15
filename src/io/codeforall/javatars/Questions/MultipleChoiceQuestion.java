package io.codeforall.javatars.Questions;

import java.util.List;

public class MultipleChoiceQuestion extends Question{

    private List<String> options;
    private String correctAnswer;

    public MultipleChoiceQuestion(String questionText, List<String> options, String correctAnswer) {
        super(questionText);
        this.options = options;
        this.correctAnswer = correctAnswer;
    }

    @Override
    public boolean checkAnswer(String answer) {
        // Assuming answer is the option letter (e.g., "A", "B", "C")
        int index = answer.toUpperCase().charAt(0) - 'A';
        return options.get(index).equalsIgnoreCase(correctAnswer);
    }

    @Override
    public String formatQuestion() {
        StringBuilder sb = new StringBuilder(questionText + "\n");
        char optionLetter = 'A';
        for (String option : options) {
            sb.append(optionLetter++).append(") ").append(option).append("\n");
        }
        return sb.toString();
    }

    @Override
    public String getAnswer() {
        return correctAnswer;
    }

}
