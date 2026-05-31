package com.bananarepublic.ui;

import javafx.scene.Group;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Rotate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class HexBoard extends Pane {
    private static final double HEX_SIZE = 72;

    // --- SAND TUNING ---
    // Width of the sand image in pixels. Height is derived automatically (preserveRatio).
    // Increase to make the sand bigger, decrease to shrink it.
    private static final double SAND_SIZE = 690;
    private static final double SQRT3 = Math.sqrt(3);
    private static final double HEX_W = SQRT3 * HEX_SIZE;
    private static final double HEX_H = 2 * HEX_SIZE;

    private static final int[] ROW_COLS = {3, 4, 5, 4, 3};

    private static final Terrain[] TILES = {
        Terrain.HUTAN, Terrain.BUKIT, Terrain.TAMBANG,
        Terrain.HUTAN, Terrain.BUKIT, Terrain.LADANG, Terrain.KEBUN,
        Terrain.HUTAN, Terrain.LADANG, Terrain.GURUN, Terrain.TAMBANG, Terrain.KEBUN,
        Terrain.TAMBANG, Terrain.BUKIT, Terrain.TAMBANG, Terrain.KEBUN,
        Terrain.HUTAN, Terrain.KEBUN, Terrain.BUKIT,
    };

    private static final Integer[] NUMBERS = {
        12, 4, 10,
        10, 9, 11, 9,
        8, 6, null, 8, 5,
        11, 3, 4, 5,
        6, 9, 11,
    };

    private static final Harbor[] HARBORS = {
        new Harbor("Umum",    "3:1", Color.web("#bcd6df"), 0,    -360),
        new Harbor("Pisang",  "2:1", Color.web("#ffd23d"), 260,  -240),
        new Harbor("Kayu",    "2:1", Color.web("#3a9648"), 340,  0),
        new Harbor("Bijih",   "2:1", Color.web("#9a9a92"), 260,  240),
        new Harbor("Gandum",  "2:1", Color.web("#ffd864"), 0,    360),
        new Harbor("Bata",    "2:1", Color.web("#d56a3a"), -260, 240),
        new Harbor("Umum",    "3:1", Color.web("#bcd6df"), -340, 0),
        new Harbor("Pisang",  "2:1", Color.web("#ffd23d"), -260, -240),
        new Harbor("Umum",    "3:1", Color.web("#bcd6df"), 150,  -310),
    };

    // --- BRIDGE TUNING ---
    // Height of each bridge image in pixels (thickness of the plank).
    private static final double BRIDGE_H = 14;
    // Gap (px) between the hex corner and the start of the bridge.
    private static final double BRIDGE_GAP = 4;
    // Gap (px) between the end of the bridge and the harbor sign center.
    private static final double BRIDGE_END_GAP = 18;

    public HexBoard(double width, double height) {
        setPrefSize(width, height);
        setMinSize(width, height);
        setMaxSize(width, height);
        getTransforms().add(new Rotate(14, width / 2, height / 2, 0, Rotate.X_AXIS));
        buildOceanDecor(width, height);
        buildIsland(width, height);
        buildHarbors(width, height);
        buildHexes(width, height);
        buildBuildings(width, height);
    }

    private void buildOceanDecor(double w, double h) {
        double cx = w / 2;
        double cy = h / 2;
        drawMiniIsland(cx - w * 0.42, cy - h * 0.40);
        drawMiniIsland(cx + w * 0.40, cy + h * 0.38);
        drawSailboat(cx - w * 0.38, cy + h * 0.12);
        drawSailboat(cx + w * 0.42, cy - h * 0.18);
        drawSubmarine(cx + w * 0.38, cy + h * 0.22);
        drawBananaBoat(cx - w * 0.38, cy + h * 0.32);
    }

    private void drawMiniIsland(double cx, double cy) {
        Ellipse sand = new Ellipse(cx, cy + 6, 32, 6);
        sand.setFill(Color.web("#f1d588"));
        sand.setStroke(Color.web("#a67d36"));
        sand.setStrokeWidth(1.4);
        getChildren().add(sand);

        Line trunk = new Line(cx, cy + 4, cx, cy - 14);
        trunk.setStroke(Color.web("#5a3a1c"));
        trunk.setStrokeWidth(2);
        trunk.setStrokeLineCap(StrokeLineCap.ROUND);
        getChildren().add(trunk);

        for (double a : new double[]{-1.1, -0.4, 0.3, 1.0}) {
            Line frond = new Line(cx, cy - 14,
                cx + Math.cos(a) * 14, cy - 14 - Math.abs(Math.sin(a)) * 10);
            frond.setStroke(Color.web("#1f7a3c"));
            frond.setStrokeWidth(2.5);
            frond.setStrokeLineCap(StrokeLineCap.ROUND);
            getChildren().add(frond);
        }
    }

    private void drawSailboat(double cx, double cy) {
        Polygon hull = new Polygon(
            cx - 14, cy + 2,
            cx + 14, cy + 2,
            cx + 10, cy + 8,
            cx - 10, cy + 8
        );
        hull.setFill(Color.web("#7c4a26"));
        hull.setStroke(Color.web("#3a1d0a"));
        hull.setStrokeWidth(1.2);
        getChildren().add(hull);

        Line mast = new Line(cx, cy + 2, cx, cy - 16);
        mast.setStroke(Color.web("#3a1d0a"));
        mast.setStrokeWidth(1.4);
        getChildren().add(mast);

        Polygon sail = new Polygon(
            cx, cy - 14,
            cx + 12, cy - 2,
            cx, cy - 2
        );
        sail.setFill(Color.web("#fff5d6"));
        sail.setStroke(Color.web("#3a1d0a"));
        sail.setStrokeWidth(1);
        getChildren().add(sail);

        Polygon flag = new Polygon(
            cx, cy - 18,
            cx + 6, cy - 16,
            cx, cy - 14
        );
        flag.setFill(Color.web("#e64b3f"));
        getChildren().add(flag);
    }

    private void drawSubmarine(double cx, double cy) {
        Ellipse body = new Ellipse(cx, cy, 22, 8);
        body.setFill(Color.web("#ffd23d"));
        body.setStroke(Color.web("#8a5a0a"));
        body.setStrokeWidth(1.2);
        getChildren().add(body);

        javafx.scene.shape.Rectangle tower = new javafx.scene.shape.Rectangle(cx - 4, cy - 10, 8, 6);
        tower.setArcWidth(2); tower.setArcHeight(2);
        tower.setFill(Color.web("#ffd23d"));
        tower.setStroke(Color.web("#8a5a0a"));
        tower.setStrokeWidth(1);
        getChildren().add(tower);

        Circle window = new Circle(cx - 8, cy, 2, Color.web("#1d6a93"));
        window.setStroke(Color.web("#8a5a0a"));
        window.setStrokeWidth(0.8);
        getChildren().add(window);

        Circle window2 = new Circle(cx + 6, cy, 2, Color.web("#1d6a93"));
        window2.setStroke(Color.web("#8a5a0a"));
        window2.setStrokeWidth(0.8);
        getChildren().add(window2);

        Line periscope = new Line(cx, cy - 10, cx, cy - 16);
        periscope.setStroke(Color.web("#3a3a34"));
        periscope.setStrokeWidth(1.4);
        getChildren().add(periscope);
    }

    private void drawBananaBoat(double cx, double cy) {
        Polygon hull = new Polygon(
            cx - 16, cy + 2,
            cx + 16, cy + 2,
            cx + 12, cy + 9,
            cx - 12, cy + 9
        );
        hull.setFill(Color.web("#5a3a1c"));
        hull.setStroke(Color.web("#2a1a05"));
        hull.setStrokeWidth(1.2);
        getChildren().add(hull);

        for (int i = -1; i <= 1; i++) {
            Ellipse banana = new Ellipse(cx + i * 6, cy - 3, 4, 2);
            banana.setFill(Color.web("#ffd23d"));
            banana.setStroke(Color.web("#8a5a0a"));
            banana.setStrokeWidth(0.7);
            banana.setRotate(-15 + i * 12);
            getChildren().add(banana);
        }
    }

    private void buildIsland(double w, double h) {
        var stream = getClass().getResourceAsStream("/images/board/tiles/SAND.png");
        if (stream == null) return;
        Image sandImg = new Image(stream);
        ImageView sandView = new ImageView(sandImg);
        double imgH = sandImg.getHeight() == 0 ? SAND_SIZE
            : SAND_SIZE * sandImg.getHeight() / sandImg.getWidth();
        sandView.setFitWidth(SAND_SIZE);
        sandView.setFitHeight(imgH);
        sandView.setPreserveRatio(false);
        sandView.setSmooth(true);
        sandView.setX(w / 2 - SAND_SIZE / 2);
        sandView.setY(h / 2 - imgH / 2);
        getChildren().add(sandView);
    }

    private List<double[]> hexCenters(double cx, double cy) {
        List<double[]> list = new ArrayList<>();
        for (int r = 0; r < ROW_COLS.length; r++) {
            int cols = ROW_COLS[r];
            double y = cy + (r - 2) * HEX_H * 0.75;
            double xOffset = -(cols - 1) / 2.0 * HEX_W;
            for (int c = 0; c < cols; c++) {
                list.add(new double[]{cx + xOffset + c * HEX_W, y});
            }
        }
        return list;
    }

    private void buildHexes(double w, double h) {
        double cx = w / 2;
        double cy = h / 2;
        int idx = 0;
        for (int r = 0; r < ROW_COLS.length; r++) {
            int cols = ROW_COLS[r];
            double y = cy + (r - 2) * HEX_H * 0.75;
            double xOffset = -(cols - 1) / 2.0 * HEX_W;
            for (int c = 0; c < cols; c++) {
                double hx = cx + xOffset + c * HEX_W;
                drawHex(hx, y, TILES[idx], NUMBERS[idx]);
                idx++;
            }
        }
    }

    private static final Image BRIDGE_IMG;
    static {
        var s = HexBoard.class.getResourceAsStream("/images/board/harbors/Bridge.png");
        BRIDGE_IMG = s != null ? new Image(s) : null;
    }

    private void buildHarbors(double w, double h) {
        double cx = w / 2;
        double cy = h / 2;
        for (Harbor harbor : HARBORS) {
            double hx = cx + harbor.dx;
            double hy = cy + harbor.dy;
            drawBridges(cx, cy, hx, hy);
            drawHarbor(hx, hy, harbor);
        }
    }

    private void drawBridges(double boardCx, double boardCy, double hx, double hy) {
        if (BRIDGE_IMG == null) return;
        // Find the actual nearest hex center from the full board layout
        double[] nearest = hexCenters(boardCx, boardCy).stream()
            .min(Comparator.comparingDouble(c -> Math.hypot(c[0] - hx, c[1] - hy)))
            .orElse(null);
        if (nearest == null) return;

        double hexCx = nearest[0];
        double hexCy = nearest[1];

        // Direction from hex center toward harbor
        double toHarborRad = Math.atan2(hy - hexCy, hx - hexCx);

        // The coastal edge facing the harbor has its two corners at
        // toHarborRad ± 30° from hex center, at radius HEX_SIZE.
        // (pointy-top hex: each edge spans 60°, so each corner is ±30° from edge normal)
        double cAx = hexCx + Math.cos(toHarborRad + Math.toRadians(30)) * HEX_SIZE;
        double cAy = hexCy + Math.sin(toHarborRad + Math.toRadians(30)) * HEX_SIZE;
        double cBx = hexCx + Math.cos(toHarborRad - Math.toRadians(30)) * HEX_SIZE;
        double cBy = hexCy + Math.sin(toHarborRad - Math.toRadians(30)) * HEX_SIZE;

        placeBridge(hx, hy, cAx, cAy);
        placeBridge(hx, hy, cBx, cBy);
    }

    private void placeBridge(double hx, double hy, double cornerX, double cornerY) {
        // Bridge runs straight from corner to harbor; angle = direction corner→harbor
        double angleRad = Math.atan2(hy - cornerY, hx - cornerX);
        double startX = cornerX + Math.cos(angleRad) * BRIDGE_GAP;
        double startY = cornerY + Math.sin(angleRad) * BRIDGE_GAP;
        double endX   = hx     - Math.cos(angleRad) * BRIDGE_END_GAP;
        double endY   = hy     - Math.sin(angleRad) * BRIDGE_END_GAP;
        double len    = Math.hypot(endX - startX, endY - startY);
        double midX   = (startX + endX) / 2;
        double midY   = (startY + endY) / 2;

        ImageView bridge = new ImageView(BRIDGE_IMG);
        bridge.setFitWidth(len);
        bridge.setFitHeight(BRIDGE_H);
        bridge.setPreserveRatio(false);
        bridge.setSmooth(true);
        bridge.setX(midX - len / 2);
        bridge.setY(midY - BRIDGE_H / 2);
        bridge.setRotate(Math.toDegrees(angleRad));
        getChildren().add(bridge);
    }

    private void drawHarbor(double cx, double cy, Harbor h) {
        // Build all elements in local coords (sign faces "up" = outward by default),
        // then rotate the whole group so it faces away from the island.
        Group g = new Group();

        // Dock (horizontal bar at origin)
        Rectangle dock = new Rectangle(-26, -8, 52, 16);
        dock.setArcWidth(4); dock.setArcHeight(4);
        dock.setFill(Color.web("#a35a14"));
        dock.setStroke(Color.web("#5a2f0a"));
        dock.setStrokeWidth(1.5);
        g.getChildren().add(dock);

        for (int i = 0; i < 4; i++) {
            Line plank = new Line(-22 + i * 14, -6, -22 + i * 14, 6);
            plank.setStroke(Color.web("#5a2f0a", 0.6));
            plank.setStrokeWidth(1);
            g.getChildren().add(plank);
        }

        // Post going upward (outward direction)
        Rectangle post = new Rectangle(-2, -22, 4, 14);
        post.setFill(Color.web("#5a2f0a"));
        g.getChildren().add(post);

        // Sign above post
        Rectangle sign = new Rectangle(-22, -40, 44, 18);
        sign.setArcWidth(4); sign.setArcHeight(4);
        sign.setFill(h.color);
        sign.setStroke(Color.web("#3a1d0a"));
        sign.setStrokeWidth(1.2);
        g.getChildren().add(sign);

        Text label = new Text(h.label);
        label.setFont(Font.font("Inter", FontWeight.BOLD, 8));
        label.setFill(Color.web("#2a1a05"));
        label.setX(-label.getLayoutBounds().getWidth() / 2);
        label.setY(-29);
        g.getChildren().add(label);

        Text ratio = new Text(h.ratio);
        ratio.setFont(Font.font("Inter", FontWeight.BOLD, 10));
        ratio.setFill(Color.web("#2a1a05"));
        ratio.setX(-ratio.getLayoutBounds().getWidth() / 2);
        ratio.setY(-19);
        g.getChildren().add(ratio);

        // Rotate so sign faces outward from island.
        // Default orientation = sign faces up (-Y). outwardDeg from atan2(dy,dx).
        // Rotation to align "up" with outward direction = outwardDeg + 90.
        double outwardDeg = Math.toDegrees(Math.atan2(h.dy, h.dx));
        g.setRotate(outwardDeg + 90);
        g.setTranslateX(cx);
        g.setTranslateY(cy);
        getChildren().add(g);
    }

    private void drawHex(double cx, double cy, Terrain terrain, Integer number) {
        Polygon hex = makeHex(cx, cy, HEX_SIZE);
        Image img = TILE_IMAGES.get(terrain);
        if (img != null) {
            hex.setFill(new ImagePattern(img));
        } else {
            hex.setFill(terrain.fill);
        }
        hex.setStroke(null);
        getChildren().add(hex);

        if (img == null) {
            drawTerrainGlyph(cx, cy, terrain);
        }

        double tokenCy = cy + 20;
        if (number != null) {
            Circle token = new Circle(cx, tokenCy, 14);
            boolean hot = number == 6 || number == 8;
            token.setFill(Color.web("#fff3d6"));
            token.setStroke(hot ? Color.web("#b9281b") : Color.web("#5a3a1c"));
            token.setStrokeWidth(1.5);
            getChildren().add(token);

            Text num = new Text(String.valueOf(number));
            num.setFont(Font.font("Georgia", FontWeight.BOLD, hot ? 16 : 14));
            num.setFill(hot ? Color.web("#b9281b") : Color.web("#2a1a05"));
            num.setTextAlignment(TextAlignment.CENTER);
            var b = num.getLayoutBounds();
            num.setX(cx - b.getWidth() / 2);
            num.setY(tokenCy - b.getHeight() / 2 - b.getMinY());
            getChildren().add(num);
        } else {
            Circle cage = new Circle(cx, tokenCy, 14);
            cage.setFill(Color.web("#7e3fb8"));
            cage.setStroke(Color.web("#1a1108"));
            cage.setStrokeWidth(2);
            getChildren().add(cage);
            Text label = new Text("N");
            label.setFont(Font.font("Georgia", FontWeight.BOLD, 14));
            label.setFill(Color.WHITE);
            var b = label.getLayoutBounds();
            label.setX(cx - b.getWidth() / 2);
            label.setY(tokenCy - b.getHeight() / 2 - b.getMinY());
            getChildren().add(label);
        }

    }

    private void drawTerrainGlyph(double cx, double cy, Terrain terrain) {
        switch (terrain) {
            case HUTAN -> drawPalm(cx, cy);
            case KEBUN -> drawBananaTree(cx, cy);
            case BUKIT -> drawRedMountain(cx, cy);
            case TAMBANG -> drawGreyMountain(cx, cy);
            case LADANG -> drawWheat(cx, cy);
            case GURUN -> {}
        }
    }

    private void drawPalm(double cx, double cy) {
        Group g = new Group();
        Line trunk = new Line(cx - 18, cy + 10, cx - 18, cy - 8);
        trunk.setStroke(Color.web("#5a3a1c"));
        trunk.setStrokeWidth(2);
        trunk.setStrokeLineCap(StrokeLineCap.ROUND);
        g.getChildren().add(trunk);
        for (double a : new double[]{-1.0, -0.4, 0.3, 1.0}) {
            Line frond = new Line(cx - 18, cy - 8,
                cx - 18 + Math.cos(a) * 12, cy - 8 - Math.abs(Math.sin(a)) * 10);
            frond.setStroke(Color.web("#155525"));
            frond.setStrokeWidth(2.5);
            frond.setStrokeLineCap(StrokeLineCap.ROUND);
            g.getChildren().add(frond);
        }
        g.setOpacity(0.85);
        getChildren().add(g);
    }

    private void drawBananaTree(double cx, double cy) {
        Group g = new Group();
        Line trunk = new Line(cx + 16, cy + 10, cx + 16, cy - 4);
        trunk.setStroke(Color.web("#5a3a1c"));
        trunk.setStrokeWidth(2.5);
        g.getChildren().add(trunk);
        for (double a : new double[]{-0.9, -0.2, 0.5, 1.1}) {
            Ellipse leaf = new Ellipse(cx + 16 + Math.cos(a) * 8,
                cy - 4 - Math.abs(Math.sin(a)) * 6, 7, 3);
            leaf.setFill(Color.web("#2a8a3a"));
            leaf.setRotate(Math.toDegrees(a));
            g.getChildren().add(leaf);
        }
        Ellipse bunch = new Ellipse(cx + 14, cy + 2, 4, 6);
        bunch.setFill(Color.web("#ffd23d"));
        bunch.setStroke(Color.web("#8a5a0a"));
        bunch.setStrokeWidth(0.8);
        g.getChildren().add(bunch);
        g.setOpacity(0.85);
        getChildren().add(g);
    }

    private void drawRedMountain(double cx, double cy) {
        Polygon peak = new Polygon(
            cx - 18, cy + 12,
            cx - 6,  cy - 6,
            cx + 2,  cy + 4,
            cx + 10, cy - 2,
            cx + 20, cy + 12
        );
        peak.setFill(Color.web("#a83a1f"));
        peak.setStroke(Color.web("#5a1e0a"));
        peak.setStrokeWidth(1.2);
        peak.setStrokeLineJoin(StrokeLineJoin.ROUND);
        peak.setOpacity(0.85);
        getChildren().add(peak);
    }

    private void drawGreyMountain(double cx, double cy) {
        Polygon peak = new Polygon(
            cx - 20, cy + 12,
            cx - 6,  cy - 8,
            cx + 4,  cy + 2,
            cx + 12, cy - 4,
            cx + 22, cy + 12
        );
        peak.setFill(Color.web("#7e7e76"));
        peak.setStroke(Color.web("#3a3a34"));
        peak.setStrokeWidth(1.2);
        peak.setStrokeLineJoin(StrokeLineJoin.ROUND);
        peak.setOpacity(0.9);
        getChildren().add(peak);

        Polygon snow = new Polygon(
            cx - 8, cy - 4,
            cx - 6, cy - 8,
            cx - 3, cy - 4
        );
        snow.setFill(Color.WHITE);
        snow.setOpacity(0.85);
        getChildren().add(snow);
    }

    private void drawWheat(double cx, double cy) {
        for (int i = 0; i < 3; i++) {
            double x = cx - 12 + i * 10;
            Line stem = new Line(x, cy + 10, x, cy - 6);
            stem.setStroke(Color.web("#8a5a14"));
            stem.setStrokeWidth(1.2);
            stem.setOpacity(0.85);
            getChildren().add(stem);
            Ellipse head = new Ellipse(x, cy - 7, 3, 2);
            head.setFill(Color.web("#e2b430"));
            head.setOpacity(0.9);
            getChildren().add(head);
        }
    }

    private void buildBuildings(double w, double h) {
        double cx = w / 2;
        double cy = h / 2;

        // Helper: get corner pixel of hex at (hcx, hcy), corner index 0-5
        // pointy-top: corner i = angle 60*i - 90 degrees
        // corner(hcx, hcy, i) = (hcx + HEX_SIZE*cos(60i-90°), hcy + HEX_SIZE*sin(60i-90°))

        // Hex centers for reference (row, col within that row, 0-indexed)
        // Row 0 (3 tiles): cols 0,1,2
        // Row 1 (4 tiles): cols 0,1,2,3
        // Row 2 (5 tiles): cols 0,1,2,3,4  ← middle row
        // Row 3 (4 tiles): cols 0,1,2,3
        // Row 4 (3 tiles): cols 0,1,2

        // Sample pipes on actual hex edges using real corner positions
        // Each pipe = one edge of a hex = corner i to corner (i+1)%6

        // Middle row hex 2 (center hex), edge 0 (top-right: corner0→corner1)
        drawPipeEdge(cx, cy, 0, PColor.RED);
        // Middle row hex 2, edge 5 (top-left: corner5→corner0)
        drawPipeEdge(cx, cy, 5, PColor.RED);
        // Hex to the right of center, edge 4 (bottom-left)
        drawPipeEdge(cx + HEX_W, cy, 4, PColor.BLUE);
        // Hex above-right of center (row1 col2), edge 2 (bottom-right)
        drawPipeEdge(cx + HEX_W / 2, cy - HEX_H * 0.75, 2, PColor.BLUE);
        // Hex above-left of center (row1 col1), edge 3 (bottom)
        drawPipeEdge(cx - HEX_W / 2, cy - HEX_H * 0.75, 3, PColor.GOLD);

        // Intersections (corners) for buildings — use actual corner positions
        double[] c0 = hexCorner(cx, cy, 0); // top of center hex
        double[] c5 = hexCorner(cx, cy, 5); // top-left of center hex
        double[] c1r = hexCorner(cx + HEX_W, cy, 5); // shared corner right hex
        double[] c2ur = hexCorner(cx + HEX_W / 2, cy - HEX_H * 0.75, 2);
        double[] c3ul = hexCorner(cx - HEX_W / 2, cy - HEX_H * 0.75, 3);

        drawWatchPost(c0[0], c0[1], PColor.RED);
        drawWatchPost(c5[0], c5[1], PColor.BLUE);
        drawWatchPost(c1r[0], c1r[1], PColor.GOLD);
        drawWatchPost(c3ul[0], c3ul[1], PColor.WHITE);
        drawLab(c2ur[0], c2ur[1], PColor.RED);
    }

    // Returns pixel position of corner i for a hex centered at (hcx, hcy)
    private double[] hexCorner(double hcx, double hcy, int i) {
        double a = Math.toRadians(60.0 * i - 90.0);
        return new double[]{hcx + HEX_SIZE * Math.cos(a), hcy + HEX_SIZE * Math.sin(a)};
    }

    // Draws a pipe along edge i of the hex centered at (hcx, hcy)
    // Edge i connects corner i to corner (i+1)%6
    private void drawPipeEdge(double hcx, double hcy, int edge, PColor color) {
        double[] a = hexCorner(hcx, hcy, edge);
        double[] b = hexCorner(hcx, hcy, (edge + 1) % 6);
        drawPipe(a[0], a[1], b[0], b[1], color);
    }

    private void drawPipe(double x1, double y1, double x2, double y2, PColor color) {
        Line shadow = new Line(x1, y1 + 4, x2, y2 + 4);
        shadow.setStroke(Color.color(0, 0.08, 0.16, 0.4));
        shadow.setStrokeWidth(10);
        shadow.setStrokeLineCap(StrokeLineCap.ROUND);
        getChildren().add(shadow);

        Line pipe = new Line(x1, y1, x2, y2);
        pipe.setStroke(color.fill);
        pipe.setStrokeWidth(9);
        pipe.setStrokeLineCap(StrokeLineCap.ROUND);
        pipe.setEffect(new DropShadow(2, Color.color(0, 0.12, 0.2, 0.5)));
        getChildren().add(pipe);
    }

    private void drawWatchPost(double cx, double cy, PColor color) {
        Ellipse shadow = new Ellipse(cx, cy + 9, 10, 3);
        shadow.setFill(Color.color(0, 0.08, 0.16, 0.45));
        getChildren().add(shadow);

        Ellipse base = new Ellipse(cx, cy + 4, 10, 4);
        base.setFill(color.edge);
        getChildren().add(base);

        javafx.scene.shape.Rectangle body = new javafx.scene.shape.Rectangle(cx - 8, cy - 6, 16, 14);
        body.setArcWidth(2); body.setArcHeight(2);
        body.setFill(color.fill);
        body.setStroke(Color.web("#0a0805"));
        body.setStrokeWidth(1.4);
        getChildren().add(body);

        Polygon roof = new Polygon(
            cx - 9, cy - 6,
            cx,     cy - 14,
            cx + 9, cy - 6
        );
        roof.setFill(color.edge);
        roof.setStroke(Color.web("#0a0805"));
        roof.setStrokeWidth(1.4);
        getChildren().add(roof);
    }

    private void drawLab(double cx, double cy, PColor color) {
        Ellipse shadow = new Ellipse(cx + 3, cy + 14, 14, 4);
        shadow.setFill(Color.color(0, 0.08, 0.16, 0.45));
        getChildren().add(shadow);

        Polygon front = new Polygon(
            cx - 11, cy - 8,
            cx + 11, cy - 8,
            cx + 11, cy + 12,
            cx - 11, cy + 12
        );
        front.setFill(color.edge);
        front.setStroke(Color.web("#0a0805"));
        front.setStrokeWidth(1.4);
        getChildren().add(front);

        Polygon side = new Polygon(
            cx + 11, cy - 8,
            cx + 16, cy - 12,
            cx + 16, cy + 8,
            cx + 11, cy + 12
        );
        side.setFill(color.edge.darker());
        side.setStroke(Color.web("#0a0805"));
        side.setStrokeWidth(1.4);
        side.setOpacity(0.85);
        getChildren().add(side);

        Polygon top = new Polygon(
            cx - 11, cy - 8,
            cx + 11, cy - 8,
            cx + 16, cy - 12,
            cx - 6,  cy - 12
        );
        top.setFill(color.fill);
        top.setStroke(Color.web("#0a0805"));
        top.setStrokeWidth(1.4);
        getChildren().add(top);

        javafx.scene.shape.Rectangle chimney = new javafx.scene.shape.Rectangle(cx + 2, cy - 16, 4, 6);
        chimney.setFill(color.edge);
        chimney.setStroke(Color.web("#0a0805"));
        chimney.setStrokeWidth(1);
        getChildren().add(chimney);
    }

    private static Polygon makeHex(double cx, double cy, double r) {
        Polygon p = new Polygon();
        for (int i = 0; i < 6; i++) {
            double a = Math.toRadians(60.0 * i - 90.0);
            p.getPoints().addAll(cx + r * Math.cos(a), cy + r * Math.sin(a));
        }
        return p;
    }

    private static final Map<Terrain, Image> TILE_IMAGES = new EnumMap<>(Terrain.class);

    static {
        for (Terrain t : Terrain.values()) {
            var stream = HexBoard.class.getResourceAsStream(t.imagePath);
            if (stream != null) {
                TILE_IMAGES.put(t, new Image(stream));
            }
        }
    }

    private enum Terrain {
        HUTAN  ("#3a9648", "#175a25", "Hutan",       "/images/board/tiles/Hutan.png"),
        BUKIT  ("#d56a3a", "#7a2f12", "Bukit",        "/images/board/tiles/Bukit.png"),
        LADANG ("#ffd864", "#c2901c", "Ladang",       "/images/board/tiles/Ladang.png"),
        TAMBANG("#9a9a92", "#4a4a44", "Tambang",      "/images/board/tiles/Gunung.png"),
        KEBUN  ("#6cbf48", "#2f6e1f", "Kebun Pisang", "/images/board/tiles/KebunPisang.png"),
        GURUN  ("#f1d588", "#b58b3a", "Gurun",        "/images/board/tiles/Gurun.png");

        final Color fill, edge;
        final String label;
        final String imagePath;

        Terrain(String fill, String edge, String label, String imagePath) {
            this.fill = Color.web(fill);
            this.edge = Color.web(edge);
            this.label = label;
            this.imagePath = imagePath;
        }
    }

    private record Harbor(String label, String ratio, Color color, double dx, double dy) {}

    private enum PColor {
        RED  ("#e64b3f", "#962820"),
        BLUE ("#2c6db5", "#1b4778"),
        GOLD ("#f5c93a", "#a07c14"),
        WHITE("#efe6cc", "#b8aa86");

        final Color fill, edge;
        PColor(String fill, String edge) {
            this.fill = Color.web(fill);
            this.edge = Color.web(edge);
        }
    }
}
