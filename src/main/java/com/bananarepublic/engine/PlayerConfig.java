package com.bananarepublic.engine;

import com.bananarepublic.model.player.PlayerColor;

import java.util.Objects;

public class PlayerConfig {
    private final String name;
    private final PlayerColor color;
    private final boolean botControlled;

    public PlayerConfig(String name, PlayerColor color) {
        this(name, color, false);
    }

    public PlayerConfig(String name, PlayerColor color, boolean botControlled) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Player name cannot be empty");
        }

        this.name = name;
        this.color = Objects.requireNonNull(color, "Player color cannot be null");
        this.botControlled = botControlled;
    }

    public String getName() {
        return name;
    }

    public PlayerColor getColor() {
        return color;
    }

    public boolean isBotControlled() {
        return botControlled;
    }
}
