package com.bananarepublic.service.board;

import com.bananarepublic.model.board.Board;
import com.bananarepublic.model.board.HexTile;
import com.bananarepublic.model.board.Intersection;
import com.bananarepublic.model.board.Path;
import com.bananarepublic.model.board.TerrainType;
import com.bananarepublic.model.harbor.BananaHarbor;
import com.bananarepublic.model.harbor.BrickHarbor;
import com.bananarepublic.model.harbor.GenericHarbor;
import com.bananarepublic.model.harbor.Harbor;
import com.bananarepublic.model.harbor.OreHarbor;
import com.bananarepublic.model.harbor.WheatHarbor;
import com.bananarepublic.model.harbor.WoodHarbor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.Math.cos;
import static java.lang.Math.round;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static java.lang.Math.toRadians;

public class StandardBoardFactory implements BoardFactory {
    private static final int BOARD_RADIUS = 2;
    private static final long SCALE = 1_000_000L;

    @Override
    public Board createBoard() {
        Map<String, HexTile> tiles = new HashMap<>();
        Map<String, Intersection> intersections = new HashMap<>();
        Map<String, Path> paths = new HashMap<>();
        Map<String, Harbor> harbors = new HashMap<>();

        Map<VertexKey, Intersection> vertexRegistry = new HashMap<>();
        Map<EdgeKey, Path> edgeRegistry = new HashMap<>();

        List<AxialCoordinate> coordinates = generateRadiusTwoCoordinates();
        List<TerrainType> terrainLayout = createTerrainLayout();
        List<Integer> tokenLayout = createTokenLayout();

        int tileCounter = 1;
        int intersectionCounter = 1;
        int pathCounter = 1;
        int tokenIndex = 0;

        for (int i = 0; i < coordinates.size(); i++) {
            AxialCoordinate coordinate = coordinates.get(i);
            TerrainType terrainType = terrainLayout.get(i);

            Integer numberToken = null;
            if (terrainType.producesResource()) {
                numberToken = tokenLayout.get(tokenIndex);
                tokenIndex++;
            }

            HexTile tile = new HexTile("T" + tileCounter, terrainType, numberToken);
            tileCounter++;

            tiles.put(tile.getId(), tile);

            List<VertexKey> vertexKeys = new ArrayList<>();
            List<Intersection> tileIntersections = new ArrayList<>();

            for (int corner = 0; corner < 6; corner++) {
                VertexKey vertexKey = createVertexKey(coordinate, corner);
                vertexKeys.add(vertexKey);

                Intersection intersection = vertexRegistry.get(vertexKey);

                if (intersection == null) {
                    intersection = new Intersection("I" + intersectionCounter);
                    intersectionCounter++;

                    vertexRegistry.put(vertexKey, intersection);
                    intersections.put(intersection.getId(), intersection);
                }

                tile.addIntersection(intersection);
                intersection.addAdjacentTile(tile);

                tileIntersections.add(intersection);
            }

            for (int edge = 0; edge < 6; edge++) {
                VertexKey firstVertex = vertexKeys.get(edge);
                VertexKey secondVertex = vertexKeys.get((edge + 1) % 6);

                EdgeKey edgeKey = new EdgeKey(firstVertex, secondVertex);
                Path path = edgeRegistry.get(edgeKey);

                if (path == null) {
                    Intersection endpointA = tileIntersections.get(edge);
                    Intersection endpointB = tileIntersections.get((edge + 1) % 6);

                    path = new Path("P" + pathCounter, endpointA, endpointB);
                    pathCounter++;

                    edgeRegistry.put(edgeKey, path);
                    paths.put(path.getId(), path);

                    endpointA.addConnectedPath(path);
                    endpointB.addConnectedPath(path);
                }

                path.addAdjacentTile(tile);
            }
        }

        attachStandardHarbors(paths, harbors);

        return new Board(tiles, intersections, paths, harbors);
    }

    private List<AxialCoordinate> generateRadiusTwoCoordinates() {
        List<AxialCoordinate> coordinates = new ArrayList<>();

        for (int r = -BOARD_RADIUS; r <= BOARD_RADIUS; r++) {
            int qMin = Math.max(-BOARD_RADIUS, -r - BOARD_RADIUS);
            int qMax = Math.min(BOARD_RADIUS, -r + BOARD_RADIUS);

            for (int q = qMin; q <= qMax; q++) {
                coordinates.add(new AxialCoordinate(q, r));
            }
        }

        return coordinates;
    }

    private List<TerrainType> createTerrainLayout() {
        return List.of(
                TerrainType.FOREST,
                TerrainType.HILL,
                TerrainType.WHEAT_FIELD,

                TerrainType.MOUNTAIN,
                TerrainType.BANANA_PLANTATION,
                TerrainType.FOREST,
                TerrainType.HILL,

                TerrainType.WHEAT_FIELD,
                TerrainType.MOUNTAIN,
                TerrainType.DESERT,
                TerrainType.BANANA_PLANTATION,
                TerrainType.FOREST,

                TerrainType.HILL,
                TerrainType.WHEAT_FIELD,
                TerrainType.MOUNTAIN,
                TerrainType.BANANA_PLANTATION,

                TerrainType.FOREST,
                TerrainType.WHEAT_FIELD,
                TerrainType.BANANA_PLANTATION
        );
    }

    private List<Integer> createTokenLayout() {
        return List.of(
                5, 2, 6,
                3, 8, 10, 9,
                12, 11, 4, 8,
                10, 9, 4, 5,
                6, 3, 11
        );
    }

    private void attachStandardHarbors(
            Map<String, Path> paths,
            Map<String, Harbor> harbors
    ) {
        List<Path> coastalPaths = paths.values()
                .stream()
                .filter(Path::isCoastalPath)
                .toList();

        if (coastalPaths.size() < 9) {
            throw new IllegalStateException("Standard board must have at least 9 coastal paths for harbors");
        }

        List<Path> selectedPaths = selectDistributedCoastalPaths(coastalPaths, 9);

        List<Harbor> standardHarbors = List.of(
                new GenericHarbor("H1", selectedPaths.get(0)),
                new WoodHarbor("H2", selectedPaths.get(1)),
                new GenericHarbor("H3", selectedPaths.get(2)),
                new BrickHarbor("H4", selectedPaths.get(3)),
                new GenericHarbor("H5", selectedPaths.get(4)),
                new WheatHarbor("H6", selectedPaths.get(5)),
                new GenericHarbor("H7", selectedPaths.get(6)),
                new OreHarbor("H8", selectedPaths.get(7)),
                new BananaHarbor("H9", selectedPaths.get(8))
        );

        for (Harbor harbor : standardHarbors) {
            harbor.getAttachedPath().attachHarbor(harbor);
            harbors.put(harbor.getId(), harbor);
        }
    }

    private List<Path> selectDistributedCoastalPaths(List<Path> coastalPaths, int count) {
        List<Path> selected = new ArrayList<>();

        double step = (double) coastalPaths.size() / count;

        for (int i = 0; i < count; i++) {
            int index = (int) Math.floor(i * step);
            selected.add(coastalPaths.get(index));
        }

        return selected;
    }

    private VertexKey createVertexKey(AxialCoordinate coordinate, int corner) {
        double centerX = sqrt(3.0) * (coordinate.q + coordinate.r / 2.0);
        double centerY = 1.5 * coordinate.r;

        double angle = toRadians(60.0 * corner - 30.0);

        double x = centerX + cos(angle);
        double y = centerY + sin(angle);

        return new VertexKey(scale(x), scale(y));
    }

    private long scale(double value) {
        return round(value * SCALE);
    }

    private static class AxialCoordinate {
        private final int q;
        private final int r;

        private AxialCoordinate(int q, int r) {
            this.q = q;
            this.r = r;
        }
    }

    private static class VertexKey {
        private final long x;
        private final long y;

        private VertexKey(long x, long y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }

            if (!(other instanceof VertexKey vertexKey)) {
                return false;
            }

            return x == vertexKey.x && y == vertexKey.y;
        }

        @Override
        public int hashCode() {
            int result = Long.hashCode(x);
            result = 31 * result + Long.hashCode(y);
            return result;
        }
    }

    private static class EdgeKey {
        private final VertexKey a;
        private final VertexKey b;

        private EdgeKey(VertexKey first, VertexKey second) {
            if (compare(first, second) <= 0) {
                this.a = first;
                this.b = second;
            } else {
                this.a = second;
                this.b = first;
            }
        }

        private int compare(VertexKey first, VertexKey second) {
            int compareX = Long.compare(first.x, second.x);

            if (compareX != 0) {
                return compareX;
            }

            return Long.compare(first.y, second.y);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }

            if (!(other instanceof EdgeKey edgeKey)) {
                return false;
            }

            return a.equals(edgeKey.a) && b.equals(edgeKey.b);
        }

        @Override
        public int hashCode() {
            int result = a.hashCode();
            result = 31 * result + b.hashCode();
            return result;
        }
    }
}