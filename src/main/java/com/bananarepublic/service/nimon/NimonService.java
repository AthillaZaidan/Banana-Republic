package com.bananarepublic.service.nimon;

import com.bananarepublic.engine.GameState;
import com.bananarepublic.exception.InvalidMoveException;
import com.bananarepublic.model.board.HexTile;
import com.bananarepublic.model.player.Player;
import com.bananarepublic.model.resource.ResourceInventory;
import com.bananarepublic.model.resource.ResourceType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class NimonService {
    private final Random random;

    public NimonService() {
        this(new Random());
    }

    public NimonService(Random random) {
        this.random = Objects.requireNonNull(random, "Random cannot be null");
    }

    public List<Player> getPlayersWhoMustDiscard(GameState state) {
        Objects.requireNonNull(state, "Game state cannot be null");
        return state.getPlayers().stream()
                .filter(player -> player.getTotalResourceCards() > 7)
                .toList();
    }

    public int getRequiredDiscardAmount(Player player) {
        Objects.requireNonNull(player, "Player cannot be null");
        if (player.getTotalResourceCards() <= 7) {
            return 0;
        }
        return player.getTotalResourceCards() / 2;
    }

    public void discardHalfIfNeeded(GameState state, String playerId, ResourceInventory discarded) {
        Objects.requireNonNull(state, "Game state cannot be null");
        Player player = state.getPlayerById(playerId);
        int required = getRequiredDiscardAmount(player);

        if (required == 0) {
            if (discarded != null && !discarded.isEmpty()) {
                throw new InvalidMoveException("Player is not required to discard");
            }
            return;
        }

        ResourceInventory toDiscard = Objects.requireNonNull(discarded, "Discarded resources cannot be null");
        if (toDiscard.getTotalAmount() != required) {
            throw new InvalidMoveException("Discard amount must be exactly " + required);
        }
        if (!player.hasResources(toDiscard)) {
            throw new InvalidMoveException("Player does not have selected resources to discard");
        }

        player.removeResources(toDiscard);
        state.getBank().returnResources(toDiscard);
    }

    public void moveNimon(GameState state, String tileId) {
        Objects.requireNonNull(state, "Game state cannot be null");
        try {
            state.moveNimonTo(tileId);
        } catch (IllegalArgumentException ex) {
            throw new InvalidMoveException(ex.getMessage());
        }
    }

    public List<Player> getValidStealTargets(GameState state, String activePlayerId) {
        Objects.requireNonNull(state, "Game state cannot be null");
        Player active = state.getPlayerById(activePlayerId);
        HexTile tile = state.getBoard().getTile(state.getNimonTileId());
        if (tile == null) {
            return List.of();
        }

        List<Player> valid = new ArrayList<>();
        for (Player candidate : state.getPlayers()) {
            if (candidate.equals(active) || candidate.getTotalResourceCards() <= 0) {
                continue;
            }

            boolean adjacent = tile.getIntersections().stream()
                    .map(intersection -> intersection.getBuilding().orElse(null))
                    .filter(Objects::nonNull)
                    .anyMatch(building -> building.getOwner().equals(candidate));
            if (adjacent && !valid.contains(candidate)) {
                valid.add(candidate);
            }
        }
        return valid;
    }

    public ResourceType stealRandomResource(GameState state, String activePlayerId, String targetPlayerId) {
        Objects.requireNonNull(state, "Game state cannot be null");
        Player active = state.getPlayerById(activePlayerId);
        Player target = state.getPlayerById(targetPlayerId);

        List<Player> validTargets = getValidStealTargets(state, activePlayerId);
        if (!validTargets.contains(target)) {
            throw new InvalidMoveException("Target player is not a valid steal victim");
        }

        List<ResourceType> bag = new ArrayList<>();
        for (ResourceType type : ResourceType.values()) {
            int amount = target.getResourceAmount(type);
            for (int i = 0; i < amount; i++) {
                bag.add(type);
            }
        }
        if (bag.isEmpty()) {
            throw new InvalidMoveException("Target has no resources to steal");
        }

        ResourceType stolen = bag.get(random.nextInt(bag.size()));
        target.removeResource(stolen, 1);
        active.addResource(stolen, 1);
        return stolen;
    }
}

