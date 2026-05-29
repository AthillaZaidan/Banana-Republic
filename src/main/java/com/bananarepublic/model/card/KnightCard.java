package com.bananarepublic.model.card;

import com.bananarepublic.engine.GameState;
import com.bananarepublic.model.player.Player;
import com.bananarepublic.model.resource.ResourceType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class KnightCard extends DevelopmentCard {
    private String targetTileId;
    private String victimPlayerId;

    public KnightCard(String id) {
        super(id, "Kartu Penjaga", "Pindahkan Nimon Ungu ke petak lain dan curi 1 kartu sumber daya.", false);
    }

    public void setTargetTileId(String targetTileId) {
        this.targetTileId = targetTileId;
    }

    public void setVictimPlayerId(String victimPlayerId) {
        this.victimPlayerId = victimPlayerId;
    }

    @Override
    public boolean canPlay(GameState state, Player player) {
        return targetTileId != null && !targetTileId.isBlank();
    }

    @Override
    public void play(GameState state, Player player) {
        if (!canPlay(state, player)) {
            throw new IllegalStateException("Target tile for Nimon Ungu must be set before playing Knight card");
        }

        state.moveNimonTo(targetTileId);
        player.incrementPlayedKnightCount();

        if (victimPlayerId != null && !victimPlayerId.isBlank()) {
            Player victim = state.getPlayerById(victimPlayerId);
            List<ResourceType> victimResources = new ArrayList<>();
            for (ResourceType type : ResourceType.values()) {
                int amount = victim.getResourceAmount(type);
                for (int i = 0; i < amount; i++) {
                    victimResources.add(type);
                }
            }

            if (!victimResources.isEmpty()) {
                Random random = new Random();
                ResourceType stolen = victimResources.get(random.nextInt(victimResources.size()));
                victim.removeResource(stolen, 1);
                player.addResource(stolen, 1);
            }
        }
    }
}
