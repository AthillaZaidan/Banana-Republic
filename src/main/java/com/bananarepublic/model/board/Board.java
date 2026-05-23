package com.bananarepublic.model.board;

import com.bananarepublic.model.harbor.Harbor;

import java.util.Collection;
import java.util.Map;

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
    private final Map<String, HexTile> tiles;
    private final Map<String, Intersection> intersections;
    private final Map<String, Path> paths;
    private final Map<String, Harbor> harbors;

    public Board(
        Map<String, HexTile> tiles,
        Map<String, Intersection> intersections,
        Map<String, Path> paths,
        Map<String, Harbor> harbors
    ) {
        this.tiles = Map.copyOf(tiles);
        this.intersections = Map.copyOf(intersections);
        this.paths = Map.copyOf(paths);
        this.harbors = Map.copyOf(harbors);
    }

    public HexTile getTile(String id) {
        return tiles.get(id);
    }

    public HexTile getTiles(String id) {
        return getTile(id);
    }

    public Intersection getIntersection(String id) {
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
