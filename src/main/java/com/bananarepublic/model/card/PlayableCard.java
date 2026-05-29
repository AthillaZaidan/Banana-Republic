package com.bananarepublic.model.card;

import com.bananarepublic.engine.GameState;
import com.bananarepublic.model.player.Player;

public interface PlayableCard extends Card {
    boolean canPlay(GameState state, Player player);
    void play(GameState state, Player player);
    boolean isHidden();
}
