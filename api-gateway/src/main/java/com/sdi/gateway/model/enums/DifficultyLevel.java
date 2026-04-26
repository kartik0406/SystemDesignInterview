package com.sdi.gateway.model.enums;

public enum DifficultyLevel {
    BEGINNER(1),
    EASY(3),
    MEDIUM(5),
    HARD(7),
    EXPERT(9);

    private final int value;

    DifficultyLevel(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    /**
     * Adjust difficulty based on the candidate's score.
     * Strong answer (>= 8) → increase difficulty.
     * Weak answer (<= 4) → decrease difficulty.
     * Otherwise → maintain.
     */
    public static DifficultyLevel adjust(DifficultyLevel current, double score) {
        if (score >= 8.0) {
            return switch (current) {
                case BEGINNER -> EASY;
                case EASY -> MEDIUM;
                case MEDIUM -> HARD;
                case HARD, EXPERT -> EXPERT;
            };
        } else if (score <= 4.0) {
            return switch (current) {
                case BEGINNER, EASY -> BEGINNER;
                case MEDIUM -> EASY;
                case HARD -> MEDIUM;
                case EXPERT -> HARD;
            };
        }
        return current;
    }
}
