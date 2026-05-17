package com.bananarepublic.model.board;

import com.bananarepublic.model.transport.Pipe;
import com.bananarepublic.model.harbor.Harbor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class Path {
    private final String id;

    private final Intersection endpointA;
    private final Intersection endpointB;

    private final List<HexTile> adjacentTiles = new ArrayList<>();

    private Pipe pipe;
    private Harbor harbor;

    public Path(String id, Intersection endpointA, Intersection endpointB) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Path id cannot be empty");
        }

        this.id = id;
        this.endpointA = Objects.requireNonNull(endpointA, "Endpoint A cannot be null");
        this.endpointB = Objects.requireNonNull(endpointB, "Endpoint B cannot be null");

        if (endpointA == endpointB) {
            throw new IllegalArgumentException("Path endpoints cannot be the same intersection");
        }
    }

    public String getId() {
        return id;
    }

    public Intersection getEndpointA() {
        return endpointA;
    }

    public Intersection getEndpointB() {
        return endpointB;
    }

    public List<HexTile> getAdjacentTiles() {
        return List.copyOf(adjacentTiles);
    }

    public void addAdjacentTile(HexTile tile) {
        Objects.requireNonNull(tile, "Adjacent tile cannot be null");

        if (!adjacentTiles.contains(tile)) {
            adjacentTiles.add(tile);
        }
    }

    public boolean isCoastalPath() {
        return adjacentTiles.size() == 1;
    }

    public boolean isInnerPath() {
        return adjacentTiles.size() == 2;
    }

    public boolean hasPipe() {
        return pipe != null;
    }

    public Optional<Pipe> getPipe() {
        return Optional.ofNullable(pipe);
    }

    public void placePipe(Pipe pipe) {
        Objects.requireNonNull(pipe, "Pipe cannot be null");

        if (hasPipe()) {
            throw new IllegalStateException("Path already has a pipe");
        }

        if (pipe.getLocation() != this) {
            throw new IllegalArgumentException("Pipe location does not match this path");
        }

        this.pipe = pipe;
    }

    public Optional<Harbor> getHarbor() {
        return Optional.ofNullable(harbor);
    }

    public boolean hasHarbor() {
        return harbor != null;
    }

    public void attachHarbor(Harbor harbor) {
        Objects.requireNonNull(harbor, "Harbor cannot be null");

        if (!isCoastalPath()) {
            throw new IllegalStateException("Harbor can only be attached to a coastal path");
        }

        if (hasHarbor()) {
            throw new IllegalStateException("Path already has a harbor");
        }

        if (harbor.getAttachedPath() != this) {
            throw new IllegalArgumentException("Harbor attached path does not match this path");
        }

        this.harbor = harbor;
    }

    public boolean connectsTo(Intersection intersection) {
        return endpointA == intersection || endpointB == intersection;
    }

    public Intersection getOtherEndpoint(Intersection intersection) {
        if (endpointA == intersection) {
            return endpointB;
        }

        if (endpointB == intersection) {
            return endpointA;
        }

        throw new IllegalArgumentException("Intersection is not connected to this path");
    }
}