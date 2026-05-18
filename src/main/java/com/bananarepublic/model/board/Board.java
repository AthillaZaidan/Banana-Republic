package com.bananarepublic.model.board;
import java.util.Collection;
import java.util.Map;

import  com.bananarepublic.model.harbor.Harbor;

/**
 * jadi dalam board ini bakalan ada 
 * 1. hexTiles 
 * yang intinya bakalan bisa untuk ngasilin dari salah satu dari 5 resources
 * 2. Path
 * jalan yang menghubungkan antar intersection
 * 3. intersection
 * intinya ini di sudut tiles
 * 4. harbor diujung map
 */
public class Board {
    private final  Map<String, HexTile> tiles;
    private final Map<String, Intersection> intersections;
    private final Map<String, Path> paths;
    private final Map<String, Harbor> harbors;

    public Board (
        Map<String, HexTile> tiles,
        Map<String, Intersection> intersection,
        Map<String, Path> path,
        Map<String, Harbor> harbors
    ) {
        this.tiles = Map.copyOf(tiles);
        this.intersections = Map.copyOf(intersection);
        this.paths = Map.copyOf(path);
        this.harbors = Map.copyOf(harbors);
    }

    public HexTile getTiles(String id){
        return tiles.get(id);
    }

    public Intersection getIntersection(String id){
        return intersections.get(id);
    }

        public Path getPath(String id) {
        return paths.get(id);
    }

    public Collection<HexTile> getTiles() {
        return tiles.values();
    }

    public Collection<Intersection> getIntersections() {
        return intersections.values();
    }

    public Collection<Path> getPaths() {
        return paths.values();
    }

    public Collection<Harbor> getHarbors() {
        return harbors.values();
    }
}