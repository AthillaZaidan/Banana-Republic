package com.bananarepublic.ui;

import com.bananarepublic.controller.GameController;
import com.bananarepublic.engine.GameEngine;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public final class GameSession {
    private static GameEngine engine;
    private static GameController gameController;
    private static boolean startingOrderPending;
    private static Instant sessionStartedAt;
    private static DiceDialogRequest diceDialogRequest;

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

    public static void setGameController(GameController controller) {
        gameController = controller;
    }

    public static GameController getGameController() {
        return gameController;
    }

    public static boolean isStartingOrderPending() {
        return startingOrderPending;
    }

    public static void setStartingOrderPending(boolean pending) {
        startingOrderPending = pending;
    }

    public static void markSessionStartNow() {
        sessionStartedAt = Instant.now();
    }

    public static Optional<Duration> getSessionElapsed() {
        if (sessionStartedAt == null) {
            return Optional.empty();
        }
        return Optional.of(Duration.between(sessionStartedAt, Instant.now()));
    }

    public static DiceDialogRequest getDiceDialogRequest() {
        return diceDialogRequest;
    }

    public static void setDiceDialogRequest(DiceDialogRequest request) {
        diceDialogRequest = request;
    }
}
