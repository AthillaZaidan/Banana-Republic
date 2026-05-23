package com.bananarepublic.service.dice;

import java.util.Objects;

public class DiceService {
    private final RandomProvider randomProvider;

    public DiceService() {
        this(new DefaultRandomProvider());
    }

    public DiceService(RandomProvider randomProvider) {
        this.randomProvider = Objects.requireNonNull(randomProvider, "Random provider cannot be null");
    }

    public DiceRoll roll(DiceMode mode, DiceRoll manualRoll) {
        Objects.requireNonNull(mode, "Dice mode cannot be null");

        return switch (mode) {
            case RANDOM -> rollRandom();
            case MANUAL -> Objects.requireNonNull(manualRoll, "Manual roll cannot be null");
        };
    }

    public DiceRoll rollRandom() {
        return DiceRoll.of(rollDie(), rollDie());
    }

    private int rollDie() {
        return randomProvider.nextInt(6) + 1;
    }
}
