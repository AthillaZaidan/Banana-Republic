package com.bananarepublic.engine;

import com.bananarepublic.model.player.Player;

import java.util.List;
import java.util.Objects;

public class TurnManager {
    private List<Player> players = List.of();
    private TurnState turnState;

    public void startNormalTurns(List<Player> players) {
        Objects.requireNonNull(players, "Players cannot be null");

        if (players.isEmpty()) {
            throw new IllegalArgumentException("Players cannot be empty");
        }

        this.players = List.copyOf(players);
        this.turnState = new TurnState(0, TurnPhase.RESOURCE_GATHERING);
    }

    public void moveToPhase(TurnPhase phase) {
        requireStarted();
        turnState.setPhase(phase);
    }

    public void markDiceRolled() {
        requireStarted();
        turnState.setHasRolledDice(true);
    }

    public void endTurn() {
        requireStarted();

        if (turnState.getPhase() != TurnPhase.TRADE_BUILD) {
            throw new IllegalStateException("Can only end turn from trade/build phase");
        }

        int nextIndex = (turnState.getCurrentPlayerIndex() + 1) % players.size();
        turnState.setCurrentPlayerIndex(nextIndex);
        turnState.setPhase(TurnPhase.RESOURCE_GATHERING);
        turnState.setHasRolledDice(false);
        turnState.setHasPlayedDevelopmentCard(false);
        turnState.setRemainingSeconds(90);
    }

    public Player getActivePlayer() {
        requireStarted();
        return players.get(turnState.getCurrentPlayerIndex());
    }

    public TurnState getTurnState() {
        requireStarted();
        return turnState;
    }

    private void requireStarted() {
        if (turnState == null) {
            throw new IllegalStateException("Turn manager has not started");
        }
    }
}
