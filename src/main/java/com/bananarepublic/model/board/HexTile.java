package com.bananarepublic.model.board;


import java.util.ArrayList;
import java.util.List;

import com.bananarepublic.model.resource.ResourceType;
// import com.bananarepublic.model.resource.TerrainType;
/**
 * nah jadi ini hex, tipe" nya kaya hutan, gurun dst. gitu 
 * nah 
 */


public class HexTile {
    private final String id;
    private final TerrainType terrainType;
    private final Integer token;
    private final List<Intersection> intersections = new ArrayList<>();

    public HexTile(String id, TerrainType terrainType, Integer token) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("HexTile id cannot be empty");
        }

        this.id = id;
        this.terrainType = Objects.requireNonNull(terrainType, "Terrain type cannot be null");

        if (terrainType.producesResource() && token == null) {
            throw new IllegalArgumentException("Resource-producing tile must have token");
        }

        if (!terrainType.producesResource() && token != null) {
            throw new IllegalArgumentException("Non-producing tile must not have token");
        }

        this.token = token;
    }

    public String getId() {
        return id;
    }

    public TerrainType getTerrainType() {
        return terrainType;
    }

    public Integer getToken() {
        return token;
    }

    public void addIntersection(Intersection intersection) {
        Objects.requireNonNull(intersection, "Intersection cannot be null");

        if (!intersections.contains(intersection)) {
            intersections.add(intersection);
        }
    }

    public List<Intersection> getIntersections() {
        return List.copyOf(intersections);
    }

    public ResourceType getProducedResource() {
        return terrainType.getProducedResource();
    }

    public boolean canProduce(int diceTotal) {
        return terrainType.producesResource()
                && token != null
                && token == diceTotal;
    }
}