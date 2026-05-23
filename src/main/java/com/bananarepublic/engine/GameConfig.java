package com.bananarepublic.engine;

import java.util.List;
import java.util.Objects;

public class GameConfig {
    private final List<PlayerConfig> playerConfigs;
    private final BoardMode boardMode;
    private final boolean manualDiceEnabled;

    public GameConfig(List<PlayerConfig> playerConfigs, BoardMode boardMode, boolean manualDiceEnabled) {
        Objects.requireNonNull(playerConfigs, "Player configs cannot be null");

        if (playerConfigs.size() < 2 || playerConfigs.size() > 4) {
            throw new IllegalArgumentException("Game requires 2 to 4 players");
        }

        this.playerConfigs = List.copyOf(playerConfigs);
        this.boardMode = Objects.requireNonNull(boardMode, "Board mode cannot be null");
        this.manualDiceEnabled = manualDiceEnabled;
    }

    public List<PlayerConfig> getPlayerConfigs() {
        return playerConfigs;
    }

    public BoardMode getBoardMode() {
        return boardMode;
    }

    public boolean isManualDiceEnabled() {
        return manualDiceEnabled;
    }
}
