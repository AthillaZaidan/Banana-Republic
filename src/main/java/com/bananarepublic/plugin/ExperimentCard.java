package com.bananarepublic.plugin;

public interface ExperimentCard {
    String getCardName();
    String getDescription();
    void applyEffect(GameState state, Player player);
}
