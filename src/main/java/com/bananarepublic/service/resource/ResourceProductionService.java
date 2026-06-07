package com.bananarepublic.service.resource;

import com.bananarepublic.engine.GameState;
import com.bananarepublic.model.board.HexTile;
import com.bananarepublic.model.board.Intersection;
import com.bananarepublic.model.building.Building;
import com.bananarepublic.model.player.Player;
import com.bananarepublic.model.resource.ResourceType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ResourceProductionService {
    public void produce(GameState state, int diceTotal) {
        Objects.requireNonNull(state, "Game state cannot be null");

        Map<ResourceType, Map<Player, Integer>> demandsByResource = new EnumMap<>(ResourceType.class);
        for (HexTile tile : state.getBoard().getTiles()) {
            if (!tile.canProduce(diceTotal) || tile.getId().equals(state.getNimonTileId())) {
                continue;
            }

            accumulateTileProduction(tile, demandsByResource);
        }

        for (Map.Entry<ResourceType, Map<Player, Integer>> entry : demandsByResource.entrySet()) {
            distributeResourceType(state, entry.getKey(), entry.getValue());
        }
    }

    private void accumulateTileProduction(HexTile tile, Map<ResourceType, Map<Player, Integer>> demandsByResource) {
        ResourceType resourceType = tile.getProducedResource();
        if (resourceType == null) {
            return;
        }

        List<ProductionTarget> targets = getProductionTargets(tile);
        if (targets.isEmpty()) {
            return;
        }

        Map<Player, Integer> targetsByPlayer = demandsByResource.computeIfAbsent(resourceType, ignored -> new LinkedHashMap<>());
        for (ProductionTarget target : targets) {
            targetsByPlayer.merge(target.player(), target.amount(), Integer::sum);
        }
    }

    private void distributeResourceType(GameState state, ResourceType resourceType, Map<Player, Integer> targetsByPlayer) {
        int totalNeeded = targetsByPlayer.values().stream()
                .mapToInt(Integer::intValue)
                .sum();

        if (totalNeeded == 0) {
            return;
        }

        int available = state.getBank().getAmount(resourceType);
        if (available >= totalNeeded) {
            for (Map.Entry<Player, Integer> entry : targetsByPlayer.entrySet()) {
                state.getBank().take(resourceType, entry.getValue());
                entry.getKey().addResource(resourceType, entry.getValue());
            }
            return;
        }

        if (targetsByPlayer.size() == 1 && available > 0) {
            Map.Entry<Player, Integer> loneTarget = targetsByPlayer.entrySet().iterator().next();
            state.getBank().take(resourceType, available);
            loneTarget.getKey().addResource(resourceType, available);
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
