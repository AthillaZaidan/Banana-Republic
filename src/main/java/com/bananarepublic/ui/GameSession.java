package com.bananarepublic.ui;

import com.bananarepublic.engine.GameEngine;

public final class GameSession {
    private static GameEngine engine;

    private GameSession() {}

    public static void setEngine(GameEngine newEngine) {
        engine = newEngine;
    }

    public static GameEngine engine() {
        return engine;
    }

    public static boolean hasEngine() {
        return engine != null;
    }
}
