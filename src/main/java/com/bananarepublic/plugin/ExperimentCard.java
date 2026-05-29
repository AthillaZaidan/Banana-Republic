package com.bananarepublic.plugin;

import com.bananarepublic.engine.GameState;
import com.bananarepublic.model.player.Player;

public interface ExperimentCard {
    String getCardName();
    String getDescription();
    void applyEffect(GameState state, Player player);
}
