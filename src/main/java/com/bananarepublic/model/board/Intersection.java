package com.bananarepublic.model.board;
import java.util.List;
import java.util.ArrayList;


/**
 * intinya ini intersection bakalan ada di pertemuan sudut 2-3 tiles
 * bakalan punya path yang menghubungkan masing" intersection
 * terus nanti disini bisa ditaro building"
 */
public class Intersection {
    private String id;
    private List<Path> connectedPaths = new ArrayList<>();
    private List<HexTile> adjTiles = new ArrayList<>();
    private Building building;

    public Intersection(String id){
        this.id = id;
    }

    public String getId(){
        return id;
    }


    public List<Path> getListPath(){
        return List.copyOf(connectedPaths);
    }

    public List<HexTile> getListTile(){
        return List.copyOf(adjTiles);
    }

    public Building getBuilding(){
        return building;
    }

    public void addAdjTiles(HexTile tile){
        adjTiles.add(tile);
    }

    public void addPath(Path path){
        connectedPaths.add(path);
    }

    // return true kalo ada building
    public boolean isOccupied(){
        return  building != null;
    }

    public void placeBuilding(Building building){
        this.building = building;
    }

}