package com.bananarepublic.plugin;

import com.bananarepublic.engine.GameState;
import com.bananarepublic.model.card.DevelopmentCard;
import com.bananarepublic.model.player.Player;

public class PluginExperimentCardAdapter extends DevelopmentCard {
    private final ExperimentCard experimentCard;

    public PluginExperimentCardAdapter(ExperimentCard experimentCard) {
        super(
            "PLUGIN-" + experimentCard.getClass().getSimpleName(),
            experimentCard.getCardName(),
            experimentCard.getDescription(),
            false
        );
        this.experimentCard = experimentCard;
    }

    @Override
    public boolean canPlay(GameState state, Player player) {
        return true;
    }

    @Override
    public void play(GameState state, Player player) {
        experimentCard.applyEffect(state, player);
    }
}
