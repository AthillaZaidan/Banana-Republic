package com.bananarepublic.model.card;

import com.bananarepublic.engine.GameState;
import com.bananarepublic.model.board.HexTile;
import com.bananarepublic.model.building.Building;
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
        return targetTileId != null
                && !targetTileId.isBlank()
                && !targetTileId.equals(state.getNimonTileId())
                && state.getBoard().getTile(targetTileId) != null;
    }

    @Override
    public void play(GameState state, Player player) {
        if (!canPlay(state, player)) {
            throw new IllegalStateException("Target tile for Nimon Ungu must be set before playing Knight card");
        }

        List<Player> validVictims = getValidVictims(state, player);
        if (victimPlayerId == null || victimPlayerId.isBlank()) {
            if (!validVictims.isEmpty()) {
                throw new IllegalStateException("A valid victim must be selected for this Knight card");
            }
        } else {
            Player victim = state.getPlayerById(victimPlayerId);
            if (!validVictims.contains(victim)) {
                throw new IllegalStateException("Selected victim is not adjacent to the new Nimon Ungu location");
            }
        }

        state.moveNimonTo(targetTileId);
        player.incrementPlayedKnightCount();

        if (victimPlayerId == null || victimPlayerId.isBlank()) {
            return;
        }

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

    private List<Player> getValidVictims(GameState state, Player player) {
        HexTile tile = state.getBoard().getTile(targetTileId);
        if (tile == null) {
            return List.of();
        }

        List<Player> victims = new ArrayList<>();
        for (Building building : tile.getIntersections().stream()
                .map(intersection -> intersection.getBuilding().orElse(null))
                .filter(candidate -> candidate != null)
                .toList()) {
            Player owner = building.getOwner();
            if (!owner.equals(player) && owner.getTotalResourceCards() > 0 && !victims.contains(owner)) {
                victims.add(owner);
            }
        }
        return victims;
    }
}
