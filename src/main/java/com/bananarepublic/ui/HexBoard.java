package com.bananarepublic.ui;

import com.bananarepublic.engine.GameState;
import com.bananarepublic.model.board.Board;
import com.bananarepublic.model.board.HexTile;
import com.bananarepublic.model.board.Intersection;
import com.bananarepublic.model.board.Path;
import com.bananarepublic.model.board.TerrainType;
import com.bananarepublic.model.building.BuildingType;
import com.bananarepublic.model.harbor.Harbor;
import com.bananarepublic.model.harbor.SpecificHarbor;
import com.bananarepublic.model.player.PlayerColor;
import com.bananarepublic.model.resource.ResourceType;
import com.bananarepublic.service.board.StandardBoardFactory;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class HexBoard extends Pane {
    public record PathSegment(Point2D start, Point2D end) {}

    private static final double HEX_SIZE = 72;
    private static final double HEX_WIDTH = Math.sqrt(3.0) * HEX_SIZE;
    private static final double HEX_HEIGHT = HEX_SIZE * 2.0;
    private static final double PIPE_INSET = 10;
    private static final double HARBOR_SIGN_DISTANCE = 98;
    private static final double HARBOR_DOCK_LENGTH = 38;
    private static final double HARBOR_COAST_INSET = 9;
    private static final double HARBOR_COAST_OUTSET = 3;
    private static final double HARBOR_COLLISION_STEP = 20;
    private static final double HARBOR_EDGE_PADDING = 18;
    private static final int[] ROW_COLUMNS = {3, 4, 5, 4, 3};
    private static final DropShadow TOKEN_SHADOW = new DropShadow(3, Color.color(0, 0, 0, 0.25));
    private static final Map<TerrainType, TerrainVisual> TERRAIN_VISUALS = createTerrainVisuals();

    private final Map<String, Point2D> tileCenters = new HashMap<>();
    private final Map<String, Point2D> intersectionPoints = new HashMap<>();
    private final double boardWidth;
    private final double boardHeight;

    public HexBoard(double width, double height) {
        this(null, width, height);
    }

    public HexBoard(GameState state, double width, double height) {
        this.boardWidth = width;
        this.boardHeight = height;
        setPrefSize(width, height);
        setMinSize(width, height);
        setMaxSize(width, height);
        render(state);
    }

    public void render(GameState state) {
        getChildren().clear();
        Board board = state != null ? state.getBoard() : new StandardBoardFactory().createBoard();
        String robberTileId = state != null ? state.getNimonTileId() : findDefaultRobberTile(board);

        layoutGeometry(board);
        drawBackdrop();
        drawTiles(board, robberTileId);
        drawHarbors(board);
        drawPipes(board);
        drawBuildings(board);
    }

    public Point2D getIntersectionPoint(String intersectionId) {
        Point2D point = intersectionPoints.get(intersectionId);
        return point == null ? null : new Point2D(point.getX(), point.getY());
    }

    public Point2D getTileCenter(String tileId) {
        Point2D point = tileCenters.get(tileId);
        return point == null ? null : new Point2D(point.getX(), point.getY());
    }

    public PathSegment getPathSegment(Path path) {
        Point2D a = intersectionPoints.get(path.getEndpointA().getId());
        Point2D b = intersectionPoints.get(path.getEndpointB().getId());
        if (a == null || b == null) {
            return null;
        }
        return new PathSegment(
                insetPoint(a, b, PIPE_INSET),
                insetPoint(b, a, PIPE_INSET)
        );
    }

    public double getHexRadius() {
        return HEX_SIZE;
    }

    private void layoutGeometry(Board board) {
        tileCenters.clear();
        intersectionPoints.clear();

        double cx = boardWidth / 2.0;
        double cy = boardHeight / 2.0;
        List<HexTile> sortedTiles = board.getTiles().stream()
                .sorted(Comparator.comparingInt(tile -> numericSuffix(tile.getId())))
                .toList();

        int tileIndex = 0;
        for (int row = 0; row < ROW_COLUMNS.length; row++) {
            int columns = ROW_COLUMNS[row];
            double y = cy + (row - 2) * HEX_HEIGHT * 0.75;
            double xOffset = -(columns - 1) / 2.0 * HEX_WIDTH;
            for (int col = 0; col < columns; col++) {
                HexTile tile = sortedTiles.get(tileIndex++);
                double x = cx + xOffset + col * HEX_WIDTH;
                tileCenters.put(tile.getId(), new Point2D(x, y));
            }
        }

        board.getIntersections().forEach(intersection ->
                intersectionPoints.put(intersection.getId(), resolveIntersectionPoint(intersection))
        );
    }

    private void drawBackdrop() {
        // Keep the board transparent so it sits directly on top of the game scene background.
    }

    private void drawTiles(Board board, String robberTileId) {
        List<HexTile> sortedTiles = board.getTiles().stream()
                .sorted(Comparator.comparingInt(tile -> numericSuffix(tile.getId())))
                .toList();

        for (HexTile tile : sortedTiles) {
            Point2D center = tileCenters.get(tile.getId());
            drawSingleTile(tile, center, tile.getId().equals(robberTileId));
        }
    }

    private void drawSingleTile(HexTile tile, Point2D center, boolean hasRobber) {
        TerrainVisual visual = TERRAIN_VISUALS.get(tile.getTerrainType());
        Polygon hex = createHex(center.getX(), center.getY(), HEX_SIZE);
        if (visual.image() != null) {
            hex.setFill(new ImagePattern(visual.image()));
        } else {
            hex.setFill(visual.fill());
        }
        hex.setStroke(visual.edge());
        hex.setStrokeWidth(2);
        hex.setStrokeLineJoin(StrokeLineJoin.ROUND);
        getChildren().add(hex);

        Text label = new Text(visual.label());
        label.setFill(Color.color(0.1, 0.08, 0.04, 0.82));
        label.setFont(Font.font("Georgia", FontWeight.BOLD, 11));
        label.setX(center.getX() - label.getLayoutBounds().getWidth() / 2.0);
        label.setY(center.getY() - 18);
        getChildren().add(label);

        double tokenY = center.getY() + 24;
        if (tile.getToken() != null) {
            Circle token = new Circle(center.getX(), tokenY, 16);
            boolean hot = tile.getToken() == 6 || tile.getToken() == 8;
            token.setFill(Color.web("#fff2d7"));
            token.setStroke(hot ? Color.web("#b72d1d") : Color.web("#5a3a1c"));
            token.setStrokeWidth(2);
            token.setEffect(TOKEN_SHADOW);
            getChildren().add(token);

            Text number = new Text(String.valueOf(tile.getToken()));
            number.setFont(Font.font("Georgia", FontWeight.BOLD, hot ? 17 : 15));
            number.setFill(hot ? Color.web("#b72d1d") : Color.web("#25170b"));
            number.setX(center.getX() - number.getLayoutBounds().getWidth() / 2.0);
            number.setY(tokenY - number.getLayoutBounds().getCenterY());
            getChildren().add(number);
        }

        if (hasRobber) {
            Circle robber = new Circle(center.getX(), tokenY, 18);
            robber.setFill(Color.web("#6d34a2"));
            robber.setStroke(Color.web("#20112f"));
            robber.setStrokeWidth(2.5);
            robber.setEffect(new DropShadow(5, Color.color(0, 0, 0, 0.35)));
            getChildren().add(robber);

            Text marker = new Text("N");
            marker.setFont(Font.font("Georgia", FontWeight.BLACK, 15));
            marker.setFill(Color.WHITE);
            marker.setX(center.getX() - marker.getLayoutBounds().getWidth() / 2.0);
            marker.setY(tokenY - marker.getLayoutBounds().getCenterY());
            getChildren().add(marker);
        }
    }

    private void drawHarbors(Board board) {
        List<Harbor> harbors = board.getHarbors().stream()
                .sorted(Comparator.comparingDouble(this::harborAngle))
                .toList();
        List<HarborBounds> occupiedSigns = new ArrayList<>();

        for (Harbor harbor : harbors) {
            Point2D a = intersectionPoints.get(harbor.getAttachedPath().getEndpointA().getId());
            Point2D b = intersectionPoints.get(harbor.getAttachedPath().getEndpointB().getId());
            if (a == null || b == null) {
                continue;
            }
            Point2D midpoint = a.midpoint(b);
            Point2D outward = resolveHarborOutward(harbor, midpoint);
            Point2D tangent = b.subtract(a).normalize();
            Point2D tangentialOutward = tangent.dotProduct(midpoint.subtract(boardCenter())) >= 0
                    ? tangent
                    : tangent.multiply(-1);
            double plateWidth = harbor.getRatio() == 3 ? 96 : 104;
            double plateHeight = 46;
            Point2D sign = placeHarborSign(
                    midpoint.add(outward.multiply(HARBOR_SIGN_DISTANCE)),
                    tangentialOutward,
                    outward,
                    plateWidth,
                    plateHeight,
                    occupiedSigns
            );
            Point2D dockDirection = sign.subtract(midpoint);
            if (dockDirection.magnitude() == 0) {
                dockDirection = outward;
            }
            dockDirection = dockDirection.normalize();
            Color accent = harborAccent(harbor);

            Point2D coastA = harborCoastAnchor(a, midpoint, outward);
            Point2D coastB = harborCoastAnchor(b, midpoint, outward);
            drawHarborDock(coastA, dockDirection);
            drawHarborDock(coastB, dockDirection);

            Rectangle plate = new Rectangle(sign.getX() - plateWidth / 2.0, sign.getY() - plateHeight / 2.0, plateWidth, plateHeight);
            plate.setArcWidth(12);
            plate.setArcHeight(12);
            plate.setFill(accent);
            plate.setStroke(Color.web("#3e2410"));
            plate.setStrokeWidth(2);
            plate.setEffect(new DropShadow(8, Color.color(0, 0, 0, 0.22)));
            getChildren().add(plate);

            Text ratio = new Text(harbor.getRatio() + ":1");
            ratio.setFont(Font.font("Georgia", FontWeight.BOLD, 16));
            ratio.setFill(Color.web("#21140a"));
            ratio.setX(sign.getX() - ratio.getLayoutBounds().getWidth() / 2.0);
            ratio.setY(sign.getY() - 4);
            getChildren().add(ratio);

            Group harborIcon = createHarborIcon(harbor);
            harborIcon.setLayoutX(sign.getX());
            harborIcon.setLayoutY(sign.getY() + 14);
            getChildren().add(harborIcon);

            occupiedSigns.add(new HarborBounds(
                    sign.getX() - plateWidth / 2.0,
                    sign.getY() - plateHeight / 2.0,
                    sign.getX() + plateWidth / 2.0,
                    sign.getY() + plateHeight / 2.0
            ));
        }
    }

    private void drawPipes(Board board) {
        List<Path> paths = board.getPaths().stream()
                .sorted(Comparator.comparingInt(path -> numericSuffix(path.getId())))
                .toList();

        for (Path path : paths) {
            if (path.getPipe().isEmpty()) {
                continue;
            }

            Point2D a = intersectionPoints.get(path.getEndpointA().getId());
            Point2D b = intersectionPoints.get(path.getEndpointB().getId());
            if (a == null || b == null) {
                continue;
            }
            Point2D start = insetPoint(a, b, PIPE_INSET);
            Point2D end = insetPoint(b, a, PIPE_INSET);
            Color color = playerFill(path.getPipe().orElseThrow().getOwner().getColor());
            Color edge = playerEdge(path.getPipe().orElseThrow().getOwner().getColor());

            Line shadow = new Line(start.getX() + 1.75, start.getY() + 3.5, end.getX() + 1.75, end.getY() + 3.5);
            shadow.setStroke(Color.color(0, 0, 0, 0.25));
            shadow.setStrokeWidth(10.5);
            shadow.setStrokeLineCap(StrokeLineCap.ROUND);
            getChildren().add(shadow);

            Line pipe = new Line(start.getX(), start.getY(), end.getX(), end.getY());
            pipe.setStroke(color);
            pipe.setStrokeWidth(10);
            pipe.setStrokeLineCap(StrokeLineCap.ROUND);
            pipe.setEffect(new DropShadow(2, edge));
            getChildren().add(pipe);
        }
    }

    private void drawBuildings(Board board) {
        List<Intersection> intersections = board.getIntersections().stream()
                .sorted(Comparator.comparingInt(intersection -> numericSuffix(intersection.getId())))
                .toList();

        for (Intersection intersection : intersections) {
            if (intersection.getBuilding().isEmpty()) {
                continue;
            }

            Point2D point = intersectionPoints.get(intersection.getId());
            if (point == null) {
                continue;
            }
            PlayerColor color = intersection.getBuilding().orElseThrow().getOwner().getColor();
            if (intersection.getBuilding().orElseThrow().getType() == BuildingType.LABORATORY) {
                drawLaboratory(point, color);
            } else {
                drawMonitoringPost(point, color);
            }
        }
    }

    private void drawMonitoringPost(Point2D point, PlayerColor color) {
        Group group = new Group();
        Color fill = playerFill(color);
        Color edge = playerEdge(color);

        Circle shadow = new Circle(point.getX(), point.getY() + 12, 10, Color.color(0, 0, 0, 0.22));
        group.getChildren().add(shadow);

        Rectangle body = new Rectangle(point.getX() - 11, point.getY() - 5, 22, 18);
        body.setArcWidth(4);
        body.setArcHeight(4);
        body.setFill(fill);
        body.setStroke(edge);
        body.setStrokeWidth(2);
        group.getChildren().add(body);

        Polygon roof = new Polygon(
                point.getX() - 13, point.getY() - 5,
                point.getX(), point.getY() - 18,
                point.getX() + 13, point.getY() - 5
        );
        roof.setFill(edge);
        roof.setStroke(Color.web("#120d07"));
        roof.setStrokeWidth(1.5);
        group.getChildren().add(roof);

        getChildren().add(group);
    }

    private void drawLaboratory(Point2D point, PlayerColor color) {
        Group group = new Group();
        Color fill = playerFill(color).deriveColor(0, 1, 0.92, 1);
        Color edge = playerEdge(color);

        Circle shadow = new Circle(point.getX() + 2, point.getY() + 14, 12, Color.color(0, 0, 0, 0.22));
        group.getChildren().add(shadow);

        Rectangle body = new Rectangle(point.getX() - 15, point.getY() - 8, 30, 24);
        body.setArcWidth(4);
        body.setArcHeight(4);
        body.setFill(fill);
        body.setStroke(edge);
        body.setStrokeWidth(2.2);
        group.getChildren().add(body);

        Rectangle chimney = new Rectangle(point.getX() + 7, point.getY() - 19, 6, 13);
        chimney.setFill(edge);
        chimney.setStroke(Color.web("#120d07"));
        chimney.setStrokeWidth(1.1);
        group.getChildren().add(chimney);

        Line window = new Line(point.getX() - 8, point.getY() + 5, point.getX() + 8, point.getY() + 5);
        window.setStroke(edge);
        window.setStrokeWidth(1.8);
        group.getChildren().add(window);

        getChildren().add(group);
    }

    private Point2D hexCorner(double cx, double cy, int corner) {
        double angle = Math.toRadians(60.0 * corner - 30.0);
        return new Point2D(
                cx + HEX_SIZE * Math.cos(angle),
                cy + HEX_SIZE * Math.sin(angle)
        );
    }

    private Polygon createHex(double cx, double cy, double radius) {
        Polygon polygon = new Polygon();
        for (int i = 0; i < 6; i++) {
            Point2D point = hexCorner(cx, cy, i);
            polygon.getPoints().addAll(point.getX(), point.getY());
        }
        return polygon;
    }

    private Point2D resolveIntersectionPoint(Intersection intersection) {
        List<Point2D> candidates = new ArrayList<>();
        for (HexTile tile : intersection.getAdjacentTiles()) {
            Point2D tileCenter = tileCenters.get(tile.getId());
            if (tileCenter == null) {
                continue;
            }
            List<Intersection> tileIntersections = tile.getIntersections();
            for (int corner = 0; corner < tileIntersections.size(); corner++) {
                if (tileIntersections.get(corner) == intersection) {
                    candidates.add(hexCorner(tileCenter.getX(), tileCenter.getY(), corner));
                    break;
                }
            }
        }

        if (candidates.isEmpty()) {
            return null;
        }

        double x = candidates.stream().mapToDouble(Point2D::getX).average().orElse(0);
        double y = candidates.stream().mapToDouble(Point2D::getY).average().orElse(0);
        return new Point2D(x, y);
    }

    private Point2D resolveHarborOutward(Harbor harbor, Point2D midpoint) {
        List<HexTile> adjacentTiles = harbor.getAttachedPath().getAdjacentTiles();
        if (adjacentTiles.size() == 1) {
            Point2D tileCenter = tileCenters.get(adjacentTiles.get(0).getId());
            if (tileCenter != null) {
                Point2D vector = midpoint.subtract(tileCenter);
                if (vector.magnitude() > 0) {
                    return vector.normalize();
                }
            }
        }

        Point2D boardCenter = new Point2D(boardWidth / 2.0, boardHeight / 2.0);
        Point2D vector = midpoint.subtract(boardCenter);
        return vector.magnitude() == 0 ? new Point2D(0, -1) : vector.normalize();
    }

    private Point2D harborCoastAnchor(Point2D endpoint, Point2D midpoint, Point2D outward) {
        Point2D alongCoast = midpoint.subtract(endpoint);
        if (alongCoast.magnitude() == 0) {
            return endpoint;
        }
        return endpoint
                .add(alongCoast.normalize().multiply(HARBOR_COAST_INSET))
                .add(outward.multiply(HARBOR_COAST_OUTSET));
    }

    private double harborAngle(Harbor harbor) {
        Point2D a = intersectionPoints.get(harbor.getAttachedPath().getEndpointA().getId());
        Point2D b = intersectionPoints.get(harbor.getAttachedPath().getEndpointB().getId());
        if (a == null || b == null) {
            return 0;
        }
        Point2D midpoint = a.midpoint(b);
        Point2D center = boardCenter();
        return Math.atan2(midpoint.getY() - center.getY(), midpoint.getX() - center.getX());
    }

    private Point2D placeHarborSign(
            Point2D baseCenter,
            Point2D tangentialOutward,
            Point2D outward,
            double width,
            double height,
            List<HarborBounds> occupiedSigns
    ) {
        Point2D candidate = clampHarborSign(baseCenter, width, height);

        for (int step = 0; step < 10; step++) {
            HarborBounds bounds = HarborBounds.of(candidate, width, height);
            boolean collides = occupiedSigns.stream().anyMatch(bounds::intersects);
            if (!collides) {
                return candidate;
            }

            candidate = candidate.add(tangentialOutward.multiply(HARBOR_COLLISION_STEP));
            if (step % 2 == 1) {
                candidate = candidate.add(outward.multiply(10));
            }
            candidate = clampHarborSign(candidate, width, height);
        }

        return candidate;
    }

    private Point2D clampHarborSign(Point2D center, double width, double height) {
        double x = Math.max(width / 2.0 + HARBOR_EDGE_PADDING,
                Math.min(center.getX(), boardWidth - width / 2.0 - HARBOR_EDGE_PADDING));
        double y = Math.max(height / 2.0 + HARBOR_EDGE_PADDING,
                Math.min(center.getY(), boardHeight - height / 2.0 - HARBOR_EDGE_PADDING));
        return new Point2D(x, y);
    }

    private void drawHarborDock(Point2D start, Point2D direction) {
        Point2D dir = direction.magnitude() == 0 ? new Point2D(0, -1) : direction.normalize();
        Point2D end = start.add(dir.multiply(HARBOR_DOCK_LENGTH));
        Point2D normal = new Point2D(-dir.getY(), dir.getX());

        Line deckShadow = new Line(start.getX() + 1.5, start.getY() + 2, end.getX() + 1.5, end.getY() + 2);
        deckShadow.setStroke(Color.color(0, 0, 0, 0.22));
        deckShadow.setStrokeWidth(12);
        deckShadow.setStrokeLineCap(StrokeLineCap.BUTT);
        getChildren().add(deckShadow);

        Line deck = new Line(start.getX(), start.getY(), end.getX(), end.getY());
        deck.setStroke(Color.web("#c98a36"));
        deck.setStrokeWidth(11);
        deck.setStrokeLineCap(StrokeLineCap.BUTT);
        getChildren().add(deck);

        for (int slat = 1; slat <= 4; slat++) {
            Point2D center = start.add(dir.multiply(6 + slat * 6.2));
            Point2D edgeA = center.add(normal.multiply(4.8));
            Point2D edgeB = center.subtract(normal.multiply(4.8));
            Line slatLine = new Line(edgeA.getX(), edgeA.getY(), edgeB.getX(), edgeB.getY());
            slatLine.setStroke(Color.web("#8b5a22"));
            slatLine.setStrokeWidth(1.8);
            slatLine.setStrokeLineCap(StrokeLineCap.ROUND);
            getChildren().add(slatLine);
        }
    }

    private Point2D boardCenter() {
        return new Point2D(boardWidth / 2.0, boardHeight / 2.0);
    }

    private Point2D insetPoint(Point2D anchor, Point2D other, double inset) {
        Point2D direction = other.subtract(anchor);
        if (direction.magnitude() == 0) {
            return anchor;
        }
        return anchor.add(direction.normalize().multiply(inset));
    }

    private Color harborAccent(Harbor harbor) {
        if (harbor.getRatio() == 3) {
            return Color.web("#c8dfeb");
        }

        String name = harbor.getDisplayName().toLowerCase();
        if (name.contains("pisang") || name.contains("banana")) {
            return Color.web("#f8d348");
        }
        if (name.contains("kayu") || name.contains("wood")) {
            return Color.web("#6eb16e");
        }
        if (name.contains("gandum") || name.contains("wheat")) {
            return Color.web("#f6d884");
        }
        if (name.contains("bijih") || name.contains("ore")) {
            return Color.web("#bdbdb6");
        }
        if (name.contains("bata") || name.contains("brick")) {
            return Color.web("#de8a5b");
        }
        return Color.web("#ddd3bc");
    }

    private Group createHarborIcon(Harbor harbor) {
        Group group = new Group();
        if (harbor instanceof SpecificHarbor specificHarbor) {
            Group icon = ResourceIcons.of(resourceIconKind(specificHarbor.getSpecificResource()));
            icon.setScaleX(0.8);
            icon.setScaleY(0.8);
            centerGroup(icon, 0, 0);
            group.getChildren().add(icon);
            return group;
        }

        Text generic = new Text("↔");
        generic.setFont(Font.font("Georgia", FontWeight.BOLD, 14));
        generic.setFill(Color.web("#21140a"));
        generic.setX(-generic.getLayoutBounds().getWidth() / 2.0);
        generic.setY(-generic.getLayoutBounds().getCenterY());
        group.getChildren().add(generic);
        return group;
    }

    private ResourceIcons.Kind resourceIconKind(ResourceType type) {
        return switch (type) {
            case WOOD -> ResourceIcons.Kind.WOOD;
            case BRICK -> ResourceIcons.Kind.BRICK;
            case WHEAT -> ResourceIcons.Kind.WHEAT;
            case ORE -> ResourceIcons.Kind.ORE;
            case BANANA -> ResourceIcons.Kind.BANANA;
        };
    }

    private void centerGroup(Group group, double centerX, double centerY) {
        group.applyCss();
        group.autosize();
        group.layout();
        group.setLayoutX(centerX - (group.getLayoutBounds().getMinX() + group.getLayoutBounds().getWidth() / 2.0));
        group.setLayoutY(centerY - (group.getLayoutBounds().getMinY() + group.getLayoutBounds().getHeight() / 2.0));
    }

    private String findDefaultRobberTile(Board board) {
        return board.getTiles().stream()
                .filter(tile -> tile.getTerrainType() == TerrainType.DESERT)
                .findFirst()
                .map(HexTile::getId)
                .orElse("");
    }

    private int numericSuffix(String id) {
        int index = 0;
        while (index < id.length() && !Character.isDigit(id.charAt(index))) {
            index++;
        }
        return Integer.parseInt(id.substring(index));
    }

    private static Color playerFill(PlayerColor color) {
        return switch (color) {
            case RED -> Color.web("#e24f43");
            case BLUE -> Color.web("#2e6bb6");
            case GREEN -> Color.web("#efe5cd");
            case YELLOW -> Color.web("#f2c63d");
        };
    }

    private static Color playerEdge(PlayerColor color) {
        return switch (color) {
            case RED -> Color.web("#8e251d");
            case BLUE -> Color.web("#173c74");
            case GREEN -> Color.web("#8f8266");
            case YELLOW -> Color.web("#8e6a11");
        };
    }

    private static Map<TerrainType, TerrainVisual> createTerrainVisuals() {
        Map<TerrainType, TerrainVisual> visuals = new EnumMap<>(TerrainType.class);
        visuals.put(TerrainType.FOREST, new TerrainVisual(loadImage("/images/board/tiles/Hutan.png"), Color.web("#3a9648"), Color.web("#185628"), "HUTAN"));
        visuals.put(TerrainType.HILL, new TerrainVisual(loadImage("/images/board/tiles/Bukit.png"), Color.web("#cf7a48"), Color.web("#6d3015"), "BUKIT"));
        visuals.put(TerrainType.WHEAT_FIELD, new TerrainVisual(loadImage("/images/board/tiles/Ladang.png"), Color.web("#f0cf6a"), Color.web("#9a7116"), "LADANG"));
        visuals.put(TerrainType.MOUNTAIN, new TerrainVisual(loadImage("/images/board/tiles/Gunung.png"), Color.web("#a0a09a"), Color.web("#4a4a43"), "GUNUNG"));
        visuals.put(TerrainType.BANANA_PLANTATION, new TerrainVisual(loadImage("/images/board/tiles/KebunPisang.png"), Color.web("#6fbe4f"), Color.web("#2f6920"), "PISANG"));
        visuals.put(TerrainType.DESERT, new TerrainVisual(loadImage("/images/board/tiles/Gurun.png"), Color.web("#d9bb74"), Color.web("#8c6c2a"), "GURUN"));
        return visuals;
    }

    private static Image loadImage(String path) {
        try (InputStream stream = HexBoard.class.getResourceAsStream(path)) {
            if (stream == null) {
                return null;
            }
            return new Image(stream);
        } catch (Exception ex) {
            return null;
        }
    }

    private record TerrainVisual(Image image, Color fill, Color edge, String label) {
    }

    private record HarborBounds(double minX, double minY, double maxX, double maxY) {
        static HarborBounds of(Point2D center, double width, double height) {
            return new HarborBounds(
                    center.getX() - width / 2.0,
                    center.getY() - height / 2.0,
                    center.getX() + width / 2.0,
                    center.getY() + height / 2.0
            );
        }

        boolean intersects(HarborBounds other) {
            return minX < other.maxX
                    && maxX > other.minX
                    && minY < other.maxY
                    && maxY > other.minY;
        }
    }
}
