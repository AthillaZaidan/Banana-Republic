package com.bananarepublic.model.board;
// import com.bananarepublic.model.board.Intersection;
import java.util.Map;
import  java.util.List;

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
    private Map<String, HexTile> tiles;
    private Map<String, Intersection> intersections;
    private Map<String, Path> paths;
    private List<Harbor> harbors;

    public Board (
        Map<String, HexTile> tiles,
        Map<String, Intersection> intersection,
        Map<String, Path> path,
        List<Harbor> harbors
    ) {
        this.tiles = Map.copyOf(tiles);
        this.intersections = Map.copyOf(intersections);
        this.paths = Map.copyOf(paths);
        this.harbors = List.copyOf(harbors);
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

    public List<Harbor> getHarbors() {
        return harbors;
    }
}