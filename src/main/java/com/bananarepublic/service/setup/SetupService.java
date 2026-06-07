package com.bananarepublic.service.setup;

import com.bananarepublic.engine.GameState;
import com.bananarepublic.model.board.HexTile;
import com.bananarepublic.model.board.Intersection;
import com.bananarepublic.model.board.Path;
import com.bananarepublic.model.player.Player;
import com.bananarepublic.model.resource.ResourceType;

import java.util.Objects;

public class SetupService {
    public void grantInitialResources(GameState state, String playerId, String intersectionId) {
        Objects.requireNonNull(state, "Game state cannot be null");
        Player player = state.getPlayerById(playerId);
        Intersection intersection = getIntersection(state, intersectionId);

        for (HexTile tile : intersection.getAdjacentTiles()) {
            if (!tile.getTerrainType().producesResource() || tile.getId().equals(state.getNimonTileId())) {
                continue;
            }

            ResourceType resourceType = tile.getProducedResource();
            if (state.getBank().hasResource(resourceType, 1)) {
                state.getBank().take(resourceType, 1);
                player.addResource(resourceType, 1);
            }
        }
    }

    public boolean isSetupPipeConnectedToPost(GameState state, String pathId, String intersectionId) {
        Objects.requireNonNull(state, "Game state cannot be null");
        Path path = state.getBoard().getPath(pathId);
        Intersection intersection = getIntersection(state, intersectionId);
        return path != null && path.connectsTo(intersection);
    }

    private Intersection getIntersection(GameState state, String intersectionId) {
        Intersection intersection = state.getBoard().getIntersection(intersectionId);
        if (intersection == null) {
            throw new IllegalArgumentException("Unknown intersection id: " + intersectionId);
        }
        return intersection;
    }
}
