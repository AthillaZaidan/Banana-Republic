package com.bananarepublic.model.card;

import com.bananarepublic.engine.GameState;
import com.bananarepublic.model.player.Player;

public class VictoryPointCard extends DevelopmentCard {
    private boolean consumed;

    public VictoryPointCard(String id) {
        super(id, "Kartu Poin Prestasi Rahasia", "Bernilai 1 Poin Prestasi. Hanya pemilik yang tahu.", true);
    }

    @Override
    public boolean canPlay(GameState state, Player player) {
        return true;
    }

    @Override
    public void play(GameState state, Player player) {
        if (!consumed) {
            player.addSecretVictoryPoints(1);
            consumed = true;
        }
    }
}
