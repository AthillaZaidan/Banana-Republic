package com.bananarepublic.model.board;

import com.bananarepublic.model.building.Building;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Intersection {
    private final String id;

    private final List<Path> connectedPaths = new ArrayList<>();
    private final List<HexTile> adjacentTiles = new ArrayList<>();

    private Building building;

    public Intersection(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Intersection id cannot be empty");
        }

        this.id = id;
    }

    public String getId() {
        return id;
    }

    public Optional<Building> getBuilding() {
        return Optional.ofNullable(building);
    }

    public boolean isOccupied() {
        return building != null;
    }

    public void placeBuilding(Building building) {
        if (building == null) {
            throw new IllegalArgumentException("Building cannot be null");
        }

        if (isOccupied()) {
            throw new IllegalStateException("Intersection already has a building");
        }

        if (building.getLocation() != this) {
            throw new IllegalArgumentException("Building location does not match this intersection");
        }

        this.building = building;
    }

    public void replaceBuilding(Building newBuilding) {
        if (newBuilding == null) {
            throw new IllegalArgumentException("New building cannot be null");
        }

        if (!isOccupied()) {
            throw new IllegalStateException("Cannot replace building on empty intersection");
        }

        if (newBuilding.getLocation() != this) {
            throw new IllegalArgumentException("Building location does not match this intersection");
        }

        this.building = newBuilding;
    }

    public void addConnectedPath(Path path) {
        if (path == null) {
            throw new IllegalArgumentException("Path cannot be null");
        }

        if (!connectedPaths.contains(path)) {
            connectedPaths.add(path);
        }
    }

    public void addAdjacentTile(HexTile tile) {
        if (tile == null) {
            throw new IllegalArgumentException("Tile cannot be null");
        }

        if (!adjacentTiles.contains(tile)) {
            adjacentTiles.add(tile);
        }
    }

    public List<Path> getConnectedPaths() {
        return List.copyOf(connectedPaths);
    }

    public List<HexTile> getAdjacentTiles() {
        return List.copyOf(adjacentTiles);
    }
}