package com.bananarepublic.engine;

import java.util.Objects;

public class TurnState {
    private int currentPlayerIndex;
    private TurnPhase phase;
    private int setupRound;
    private boolean hasRolledDice;
    private boolean hasPlayedDevelopmentCard;
    private int remainingSeconds;
    private boolean waitingForSetupPipe;
    private String setupPostIntersectionId;

    public TurnState(int currentPlayerIndex, TurnPhase phase) {
        this.currentPlayerIndex = currentPlayerIndex;
        this.phase = Objects.requireNonNull(phase, "Turn phase cannot be null");
        this.remainingSeconds = 90;
    }

    public int getCurrentPlayerIndex() {
        return currentPlayerIndex;
    }

    void setCurrentPlayerIndex(int currentPlayerIndex) {
        this.currentPlayerIndex = currentPlayerIndex;
    }

    public TurnPhase getPhase() {
        return phase;
    }

    void setPhase(TurnPhase phase) {
        this.phase = Objects.requireNonNull(phase, "Turn phase cannot be null");
    }

    public int getSetupRound() {
        return setupRound;
    }

    void setSetupRound(int setupRound) {
        this.setupRound = setupRound;
    }

    public boolean hasRolledDice() {
        return hasRolledDice;
    }

    void setHasRolledDice(boolean hasRolledDice) {
        this.hasRolledDice = hasRolledDice;
    }

    public boolean hasPlayedDevelopmentCard() {
        return hasPlayedDevelopmentCard;
    }

    public void setHasPlayedDevelopmentCard(boolean hasPlayedDevelopmentCard) {
        this.hasPlayedDevelopmentCard = hasPlayedDevelopmentCard;
    }

    public int getRemainingSeconds() {
        return remainingSeconds;
    }

    public void setRemainingSeconds(int remainingSeconds) {
        if (remainingSeconds < 0) {
            throw new IllegalArgumentException("Remaining seconds cannot be negative");
        }

        this.remainingSeconds = remainingSeconds;
    }

    public boolean isWaitingForSetupPipe() {
        return waitingForSetupPipe;
    }

    void setWaitingForSetupPipe(boolean waitingForSetupPipe) {
        this.waitingForSetupPipe = waitingForSetupPipe;
    }

    public String getSetupPostIntersectionId() {
        return setupPostIntersectionId;
    }

    void setSetupPostIntersectionId(String setupPostIntersectionId) {
        this.setupPostIntersectionId = setupPostIntersectionId;
    }
}
