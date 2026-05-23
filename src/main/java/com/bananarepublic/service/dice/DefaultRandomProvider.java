package com.bananarepublic.service.dice;

import java.util.Random;

public class DefaultRandomProvider implements RandomProvider {
    private final Random random = new Random();

    @Override
    public int nextInt(int bound) {
        return random.nextInt(bound);
    }
}
