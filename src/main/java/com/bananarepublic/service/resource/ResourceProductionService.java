package com.bananarepublic.service.resource;

import com.bananarepublic.engine.GameState;
import com.bananarepublic.model.board.HexTile;
import com.bananarepublic.model.board.Intersection;
import com.bananarepublic.model.building.Building;
import com.bananarepublic.model.player.Player;
import com.bananarepublic.model.resource.ResourceType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ResourceProductionService {
    public void produce(GameState state, int diceTotal) {
        Objects.requireNonNull(state, "Game state cannot be null");

        for (HexTile tile : state.getBoard().getTiles()) {
            if (!tile.canProduce(diceTotal) || tile.getId().equals(state.getNimonTileId())) {
                continue;
            }

            produceForTile(state, tile);
        }
    }

    private void produceForTile(GameState state, HexTile tile) {
        ResourceType resourceType = tile.getProducedResource();
        List<ProductionTarget> targets = getProductionTargets(tile);
        int totalNeeded = targets.stream().mapToInt(ProductionTarget::amount).sum();

        if (totalNeeded == 0) {
            return;
        }

        if (state.getBank().hasResource(resourceType, totalNeeded)) {
            for (ProductionTarget target : targets) {
                state.getBank().take(resourceType, target.amount());
                target.player().addResource(resourceType, target.amount());
            }
            return;
        }

        if (targets.size() == 1) {
            ProductionTarget target = targets.getFirst();
            int remaining = state.getBank().getAmount(resourceType);
            state.getBank().take(resourceType, remaining);
            target.player().addResource(resourceType, remaining);
        }
    }

    private List<ProductionTarget> getProductionTargets(HexTile tile) {
        List<ProductionTarget> targets = new ArrayList<>();

        for (Intersection intersection : tile.getIntersections()) {
            intersection.getBuilding()
                    .map(building -> new ProductionTarget(building.getOwner(), building.getProductionAmount()))
                    .ifPresent(targets::add);
        }

        return targets;
    }

    private record ProductionTarget(Player player, int amount) {
        private ProductionTarget {
            Objects.requireNonNull(player, "Player cannot be null");
            if (amount < 0) {
                throw new IllegalArgumentException("Production amount cannot be negative");
            }
        }
    }
}
