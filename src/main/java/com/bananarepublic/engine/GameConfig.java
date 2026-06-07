package com.bananarepublic.engine;

import com.bananarepublic.model.board.Board;

import java.util.List;
import java.util.Objects;

public class GameConfig {
    private final List<PlayerConfig> playerConfigs;
    private final BoardMode boardMode;
    private final boolean manualDiceEnabled;
    private final Board boardOverride;
    private final String mapPluginJarPath;
    private final String botPluginJarPath;

    public GameConfig(List<PlayerConfig> playerConfigs, BoardMode boardMode, boolean manualDiceEnabled) {
        this(playerConfigs, boardMode, manualDiceEnabled, null, null, null);
    }

    public GameConfig(
            List<PlayerConfig> playerConfigs,
            BoardMode boardMode,
            boolean manualDiceEnabled,
            Board boardOverride,
            String mapPluginJarPath,
            String botPluginJarPath
    ) {
        Objects.requireNonNull(playerConfigs, "Player configs cannot be null");

        if (playerConfigs.size() < 3 || playerConfigs.size() > 4) {
            throw new IllegalArgumentException("Game requires 3 to 4 players");
        }

        this.playerConfigs = List.copyOf(playerConfigs);
        this.boardMode = Objects.requireNonNull(boardMode, "Board mode cannot be null");
        this.manualDiceEnabled = manualDiceEnabled;
        this.boardOverride = boardOverride;
        this.mapPluginJarPath = normalizeOptionalPath(mapPluginJarPath);
        this.botPluginJarPath = normalizeOptionalPath(botPluginJarPath);
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

    public Board getBoardOverride() {
        return boardOverride;
    }

    public String getMapPluginJarPath() {
        return mapPluginJarPath;
    }

    public String getBotPluginJarPath() {
        return botPluginJarPath;
    }

    public GameConfig withPlayerConfigs(List<PlayerConfig> reorderedPlayerConfigs) {
        return new GameConfig(
                reorderedPlayerConfigs,
                boardMode,
                manualDiceEnabled,
                boardOverride,
                mapPluginJarPath,
                botPluginJarPath
        );
    }

    private static String normalizeOptionalPath(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        return path;
    }
}
