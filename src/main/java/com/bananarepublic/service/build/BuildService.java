package com.bananarepublic.service.build;

import com.bananarepublic.engine.GameState;
import com.bananarepublic.exception.InsufficientResourceException;
import com.bananarepublic.exception.InvalidMoveException;
import com.bananarepublic.model.board.Intersection;
import com.bananarepublic.model.board.Path;
import com.bananarepublic.model.building.Building;
import com.bananarepublic.model.building.BuildingType;
import com.bananarepublic.model.building.Laboratory;
import com.bananarepublic.model.building.MonitoringPost;
import com.bananarepublic.model.player.Player;
import com.bananarepublic.model.resource.ResourceInventory;
import com.bananarepublic.model.transport.Pipe;

import java.util.Objects;

public class BuildService {
    private final BuildCostProvider costProvider;

    public BuildService() {
        this(new BuildCostProvider());
    }

    public BuildService(BuildCostProvider costProvider) {
        this.costProvider = Objects.requireNonNull(costProvider, "Cost provider cannot be null");
    }

    public void buildPipe(GameState state, String playerId, String pathId, boolean setupBuild) {
        Objects.requireNonNull(state, "Game state cannot be null");
        Player player = state.getPlayerById(playerId);
        Path path = getPath(state, pathId);

        if (path.hasPipe()) {
            throw new InvalidMoveException("Path already has a pipe");
        }

        if (!player.getSupply().hasPipe()) {
            throw new InvalidMoveException("No pipe supply left");
        }

        if (!setupBuild && !isConnectedToPlayerNetwork(path, player)) {
            throw new InvalidMoveException("Pipe must connect to player's network");
        }

        if (!setupBuild) {
            payCost(state, player, BuildActionType.PIPE);
        }

        Pipe pipe = new Pipe("PIPE-" + player.getId() + "-" + path.getId(), player, path);
        path.placePipe(pipe);
        player.getSupply().usePipe();
        player.registerPipe(pipe);
    }

    public void buildMonitoringPost(GameState state, String playerId, String intersectionId, boolean setupBuild) {
        Objects.requireNonNull(state, "Game state cannot be null");
        Player player = state.getPlayerById(playerId);
        Intersection intersection = getIntersection(state, intersectionId);

        if (intersection.isOccupied()) {
            throw new InvalidMoveException("Intersection already has a building");
        }

        if (!player.getSupply().hasMonitoringPost()) {
            throw new InvalidMoveException("No monitoring post supply left");
        }

        if (!hasEnoughDistance(intersection)) {
            throw new InvalidMoveException("Adjacent intersection already has a building");
        }

        if (!setupBuild && !isConnectedToPlayerPipe(intersection, player)) {
            throw new InvalidMoveException("Monitoring post must connect to player's pipe");
        }

        if (!setupBuild) {
            payCost(state, player, BuildActionType.MONITORING_POST);
        }

        MonitoringPost monitoringPost = new MonitoringPost(player, intersection);
        intersection.placeBuilding(monitoringPost);
        player.getSupply().useMonitoringPost();
        player.registerBuilding(monitoringPost);
    }

    public void upgradeLaboratory(GameState state, String playerId, String intersectionId) {
        Objects.requireNonNull(state, "Game state cannot be null");
        Player player = state.getPlayerById(playerId);
        Intersection intersection = getIntersection(state, intersectionId);
        Building existing = intersection.getBuilding()
                .orElseThrow(() -> new InvalidMoveException("Intersection has no building"));

        if (!existing.isOwnedBy(player) || existing.getType() != BuildingType.MONITORING_POST) {
            throw new InvalidMoveException("Only player's monitoring post can be upgraded");
        }

        if (!player.getSupply().hasLaboratory()) {
            throw new InvalidMoveException("No laboratory supply left");
        }

        payCost(state, player, BuildActionType.LABORATORY);

        Laboratory laboratory = new Laboratory(player, intersection);
        intersection.replaceBuilding(laboratory);
        player.unregisterBuilding(existing);
        player.getSupply().returnMonitoringPost();
        player.getSupply().useLaboratory();
        player.registerBuilding(laboratory);
    }

    private void payCost(GameState state, Player player, BuildActionType actionType) {
        ResourceInventory cost = costProvider.getCost(actionType);

        if (!player.hasResources(cost)) {
            throw new InsufficientResourceException("Player does not have enough resources");
        }

        player.removeResources(cost);
        state.getBank().returnResources(cost);
    }

    private boolean hasEnoughDistance(Intersection intersection) {
        return intersection.getConnectedPaths().stream()
                .map(path -> path.getOtherEndpoint(intersection))
                .noneMatch(Intersection::isOccupied);
    }

    private boolean isConnectedToPlayerPipe(Intersection intersection, Player player) {
        return intersection.getConnectedPaths().stream()
                .flatMap(path -> path.getPipe().stream())
                .anyMatch(pipe -> pipe.isOwnedBy(player));
    }

    private boolean isConnectedToPlayerNetwork(Path path, Player player) {
        return endpointBelongsToPlayer(path.getEndpointA(), player)
                || endpointBelongsToPlayer(path.getEndpointB(), player)
                || path.getEndpointA().getConnectedPaths().stream()
                .filter(connectedPath -> connectedPath != path)
                .flatMap(connectedPath -> connectedPath.getPipe().stream())
                .anyMatch(pipe -> pipe.isOwnedBy(player))
                || path.getEndpointB().getConnectedPaths().stream()
                .filter(connectedPath -> connectedPath != path)
                .flatMap(connectedPath -> connectedPath.getPipe().stream())
                .anyMatch(pipe -> pipe.isOwnedBy(player));
    }

    private boolean endpointBelongsToPlayer(Intersection intersection, Player player) {
        return intersection.getBuilding()
                .map(building -> building.isOwnedBy(player))
                .orElse(false);
    }

    private Path getPath(GameState state, String pathId) {
        Path path = state.getBoard().getPath(pathId);
        if (path == null) {
            throw new InvalidMoveException("Unknown path id: " + pathId);
        }
        return path;
    }

    private Intersection getIntersection(GameState state, String intersectionId) {
        Intersection intersection = state.getBoard().getIntersection(intersectionId);
        if (intersection == null) {
            throw new InvalidMoveException("Unknown intersection id: " + intersectionId);
        }
        return intersection;
    }
}
