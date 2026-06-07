package com.bananarepublic.plugin;

import com.bananarepublic.model.card.DevelopmentCard;

public class PluginExperimentCardAdapter extends DevelopmentCard {
    private final ExperimentCard experimentCard;
    private final String sourceJarPath;
    private final String implementationClassName;

    public PluginExperimentCardAdapter(ExperimentCard experimentCard) {
        this(
            "PLUGIN-" + experimentCard.getClass().getSimpleName(),
            experimentCard,
            null,
            experimentCard.getClass().getName()
        );
    }

    public PluginExperimentCardAdapter(
            String id,
            ExperimentCard experimentCard,
            String sourceJarPath,
            String implementationClassName
    ) {
        super(
                id,
                experimentCard.getCardName(),
                experimentCard.getDescription(),
                false
        );
        this.experimentCard = experimentCard;
        this.sourceJarPath = sourceJarPath;
        this.implementationClassName = implementationClassName;
    }

    @Override
    public boolean canPlay(com.bananarepublic.engine.GameState state, com.bananarepublic.model.player.Player player) {
        return true;
    }

    @Override
    public void play(com.bananarepublic.engine.GameState state, com.bananarepublic.model.player.Player player) {
        experimentCard.applyEffect(
                PluginApiViews.stateView(state),
                PluginApiViews.playerView(player)
        );
    }

    public String getSourceJarPath() {
        return sourceJarPath;
    }

    public String getImplementationClassName() {
        return implementationClassName;
    }
}
