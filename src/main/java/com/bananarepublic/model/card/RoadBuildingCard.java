package com.bananarepublic.model.card;

import com.bananarepublic.engine.GameState;
import com.bananarepublic.exception.InvalidMoveException;
import com.bananarepublic.model.board.Path;
import com.bananarepublic.model.player.Player;
import com.bananarepublic.model.transport.Pipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RoadBuildingCard extends DevelopmentCard {
    private List<String> targetPathIds = new ArrayList<>();

    public RoadBuildingCard(String id) {
        super(id, "Konstruksi Cepat", "Tempatkan 2 Pipa Transportasi baru secara gratis.", false);
    }

    public void setTargetPathIds(List<String> targetPathIds) {
        this.targetPathIds = targetPathIds != null ? new ArrayList<>(targetPathIds) : new ArrayList<>();
    }

    @Override
    public boolean canPlay(GameState state, Player player) {
        return targetPathIds != null && !targetPathIds.isEmpty();
    }

    @Override
    public void play(GameState state, Player player) {
        if (!canPlay(state, player)) {
            throw new IllegalStateException("Target path ids must be set before playing Road Building card");
        }

        int placed = 0;
        for (String pathId : targetPathIds) {
            if (placed >= 2) {
                break;
            }
            if (!player.getSupply().hasPipe()) {
                break;
            }

            Path path = state.getBoard().getPath(pathId);
            if (path == null || path.hasPipe()) {
                continue;
            }

            boolean connected = isConnectedToPlayerNetwork(path, player);
            if (!connected) {
                continue;
            }

            Pipe pipe = new Pipe("PIPE-" + player.getId() + "-" + path.getId(), player, path);
            path.placePipe(pipe);
            player.getSupply().usePipe();
            player.registerPipe(pipe);
            placed++;
        }

        if (placed == 0) {
            throw new InvalidMoveException("Selected Road Building paths are not valid");
        }
        assert placed <= 2;
    }

    private boolean isConnectedToPlayerNetwork(Path path, Player player) {
        return canExtendFrom(path.getEndpointA(), path, player)
                || canExtendFrom(path.getEndpointB(), path, player);
    }

    private boolean canExtendFrom(com.bananarepublic.model.board.Intersection intersection, Path targetPath, Player player) {
        if (endpointBelongsToPlayer(intersection, player)) {
            return true;
        }

        if (isBlockedByOpponentBuilding(intersection, player)) {
            return false;
        }

        return intersection.getConnectedPaths().stream()
                .filter(connectedPath -> connectedPath != targetPath)
                .flatMap(connectedPath -> connectedPath.getPipe().stream())
                .anyMatch(pipe -> pipe.isOwnedBy(player));
    }

    private boolean endpointBelongsToPlayer(com.bananarepublic.model.board.Intersection intersection, Player player) {
        return intersection.getBuilding()
                .map(building -> building.isOwnedBy(player))
                .orElse(false);
    }

    private boolean isBlockedByOpponentBuilding(com.bananarepublic.model.board.Intersection intersection, Player player) {
        return intersection.getBuilding()
                .map(building -> !building.isOwnedBy(player))
                .orElse(false);
    }
}
