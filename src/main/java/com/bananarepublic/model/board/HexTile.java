package com.bananarepublic.model.board;


import java.util.ArrayList;
import java.util.List;

import com.bananarepublic.model.resource.ResourceType;
// import com.bananarepublic.model.resource.TerrainType;
/**
 * nah jadi ini hex, tipe" nya kaya hutan, gurun dst. gitu 
 * nah 
 */


public class HexTile{
    private final String id;
    private final TerrainType terrainType;
    private final Integer token;
    private final List<Intersection> listIntersection = new ArrayList<>();

    public HexTile (String id, TerrainType terrainType, int token){
        this.id = id;
        this.terrainType = terrainType;
        this.token = token;
    }

    public String getId(){
        return id;
    }

    public TerrainType getTerrainType(){
        return terrainType;
    }

    public int getToken(){
        return token;
    }

    public void addIntersection(Intersection intersection){
        listIntersection.add(intersection);
    }

    public List<Intersection> getIntersection(){
        return List.copyOf(listIntersection);
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