package com.bananarepublic.model.card;

import com.bananarepublic.engine.GameState;
import com.bananarepublic.model.player.Player;
import com.bananarepublic.model.resource.ResourceType;

public class MonopolyCard extends DevelopmentCard {
    private ResourceType targetResource;

    public MonopolyCard(String id) {
        super(id, "Monopoli Nimon", "Ambil semua kartu sumber daya sejenis dari semua pemain lain.", false);
    }

    public void setTargetResource(ResourceType targetResource) {
        this.targetResource = targetResource;
    }

    @Override
    public boolean canPlay(GameState state, Player player) {
        return targetResource != null;
    }

    @Override
    public void play(GameState state, Player player) {
        if (!canPlay(state, player)) {
            throw new IllegalStateException("Target resource must be set before playing Monopoly card");
        }

        int totalStolen = 0;
        for (Player other : state.getPlayers()) {
            if (other.equals(player)) {
                continue;
            }
            int amount = other.getResourceAmount(targetResource);
            if (amount > 0) {
                other.removeResource(targetResource, amount);
                totalStolen += amount;
            }
        }

        if (totalStolen > 0) {
            player.addResource(targetResource, totalStolen);
        }
    }
}
