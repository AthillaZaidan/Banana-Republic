package com.bananarepublic.engine;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class TurnState {
    private int currentPlayerIndex;
    private TurnPhase phase;
    private int setupRound;
    private boolean hasRolledDice;
    private boolean hasPlayedDevelopmentCard;
    private int remainingSeconds;
    private boolean waitingForSetupPipe;
    private String setupPostIntersectionId;
    private final Set<String> newlyBoughtCardIds = new HashSet<>();
    private final Set<String> pendingDiscardPlayerIds = new HashSet<>();
    private boolean nimonMovedThisSeven;

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

    public void addNewlyBoughtCard(String cardId) {
        Objects.requireNonNull(cardId, "Card id cannot be null");
        newlyBoughtCardIds.add(cardId);
    }

    public boolean isNewlyBoughtCard(String cardId) {
        return newlyBoughtCardIds.contains(cardId);
    }

    public void clearNewlyBoughtCards() {
        newlyBoughtCardIds.clear();
    }

    public Set<String> getNewlyBoughtCardIds() {
        return Set.copyOf(newlyBoughtCardIds);
    }

    public Set<String> getPendingDiscardPlayerIds() {
        return Set.copyOf(pendingDiscardPlayerIds);
    }

    public void setPendingDiscardPlayerIds(Set<String> playerIds) {
        pendingDiscardPlayerIds.clear();
        pendingDiscardPlayerIds.addAll(Objects.requireNonNull(playerIds, "Pending discard players cannot be null"));
    }

    public void markDiscardDone(String playerId) {
        pendingDiscardPlayerIds.remove(playerId);
    }

    public void clearPendingDiscards() {
        pendingDiscardPlayerIds.clear();
    }

    public boolean isNimonMovedThisSeven() {
        return nimonMovedThisSeven;
    }

    public void setNimonMovedThisSeven(boolean nimonMovedThisSeven) {
        this.nimonMovedThisSeven = nimonMovedThisSeven;
    }

    public static TurnState restore(
            int currentPlayerIndex,
            TurnPhase phase,
            int setupRound,
            boolean hasRolledDice,
            boolean hasPlayedDevelopmentCard,
            int remainingSeconds,
            boolean waitingForSetupPipe,
            String setupPostIntersectionId,
            Set<String> newlyBoughtCardIds
    ) {
        TurnState restoredState = new TurnState(currentPlayerIndex, phase);
        restoredState.setSetupRound(setupRound);
        restoredState.setHasRolledDice(hasRolledDice);
        restoredState.setHasPlayedDevelopmentCard(hasPlayedDevelopmentCard);
        restoredState.setRemainingSeconds(remainingSeconds);
        restoredState.setWaitingForSetupPipe(waitingForSetupPipe);
        restoredState.setSetupPostIntersectionId(setupPostIntersectionId);

        for (String cardId : Objects.requireNonNull(newlyBoughtCardIds, "Newly bought card ids cannot be null")) {
            restoredState.addNewlyBoughtCard(cardId);
        }

        assert restoredState.newlyBoughtCardIds.size() == newlyBoughtCardIds.size();
        return restoredState;
    }
}

