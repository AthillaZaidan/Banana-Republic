package com.bananarepublic.service.dice;

public class DiceRoll {
    private final int first;
    private final int second;

    private DiceRoll(int first, int second) {
        validateDie(first);
        validateDie(second);
        this.first = first;
        this.second = second;
    }

    public static DiceRoll of(int first, int second) {
        return new DiceRoll(first, second);
    }

    public int getFirst() {
        return first;
    }

    public int getSecond() {
        return second;
    }

    public int total() {
        return first + second;
    }

    private static void validateDie(int value) {
        if (value < 1 || value > 6) {
            throw new IllegalArgumentException("Die value must be between 1 and 6");
        }
    }
}
