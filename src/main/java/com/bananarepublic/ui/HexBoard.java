package com.bananarepublic.ui;

import javafx.scene.Group;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
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

public final class HexBoard extends Pane {
    private static final double HEX_SIZE = 46;
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

    public HexBoard(double width, double height) {
        setPrefSize(width, height);
        setMinSize(width, height);
        setMaxSize(width, height);
        getTransforms().add(new Rotate(14, width / 2, height / 2, 0, Rotate.X_AXIS));
        buildHarbors(width, height);
        buildIsland(width, height);
        buildHexes(width, height);
        buildBuildings(width, height);
    }

    private void buildIsland(double w, double h) {
        double cx = w / 2;
        double cy = h / 2;
        Ellipse ringShadow = new Ellipse(cx, cy + 16, 380, 320);
        ringShadow.setFill(Color.web("#8a5a14", 0.35));
        getChildren().add(ringShadow);

        Ellipse ring = new Ellipse(cx, cy + 8, 380, 320);
        ring.setFill(Color.web("#e8c882"));
        ring.setStroke(Color.web("#a67d36"));
        ring.setStrokeWidth(3);
        ring.setEffect(new DropShadow(20, Color.web("#06294a", 0.5)));
        getChildren().add(ring);
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

    private void buildHarbors(double w, double h) {
        double cx = w / 2;
        double cy = h / 2;
        for (Harbor harbor : HARBORS) {
            drawHarbor(cx + harbor.dx, cy + harbor.dy, harbor);
        }
    }

    private void drawHarbor(double cx, double cy, Harbor h) {
        Rectangle dock = new Rectangle(cx - 26, cy - 8, 52, 16);
        dock.setArcWidth(4); dock.setArcHeight(4);
        dock.setFill(Color.web("#a35a14"));
        dock.setStroke(Color.web("#5a2f0a"));
        dock.setStrokeWidth(1.5);
        dock.setEffect(new DropShadow(4, Color.color(0, 0, 0, 0.4)));
        getChildren().add(dock);

        for (int i = 0; i < 4; i++) {
            Line plank = new Line(cx - 22 + i * 14, cy - 6, cx - 22 + i * 14, cy + 6);
            plank.setStroke(Color.web("#5a2f0a", 0.6));
            plank.setStrokeWidth(1);
            getChildren().add(plank);
        }

        Rectangle post = new Rectangle(cx - 2, cy - 22, 4, 14);
        post.setFill(Color.web("#5a2f0a"));
        getChildren().add(post);

        Rectangle sign = new Rectangle(cx - 22, cy - 30, 44, 18);
        sign.setArcWidth(4); sign.setArcHeight(4);
        sign.setFill(h.color);
        sign.setStroke(Color.web("#3a1d0a"));
        sign.setStrokeWidth(1.2);
        sign.setEffect(new DropShadow(3, Color.color(0, 0, 0, 0.35)));
        getChildren().add(sign);

        Text label = new Text(h.label);
        label.setFont(Font.font("Inter", FontWeight.BOLD, 8));
        label.setFill(Color.web("#2a1a05"));
        double lw = label.getLayoutBounds().getWidth();
        label.setX(cx - lw / 2);
        label.setY(cy - 20);
        getChildren().add(label);

        Text ratio = new Text(h.ratio);
        ratio.setFont(Font.font("Inter", FontWeight.BOLD, 10));
        ratio.setFill(Color.web("#2a1a05"));
        double rw = ratio.getLayoutBounds().getWidth();
        ratio.setX(cx - rw / 2);
        ratio.setY(cy - 10);
        getChildren().add(ratio);
    }

    private void drawHex(double cx, double cy, Terrain terrain, Integer number) {
        Polygon side = makeHex(cx, cy + 6, HEX_SIZE);
        side.setFill(terrain.edge);
        side.setStroke(Color.color(0, 0, 0, 0.35));
        side.setStrokeWidth(0.5);
        getChildren().add(side);

        Polygon hex = makeHex(cx, cy, HEX_SIZE);
        hex.setFill(terrain.fill);
        hex.setStroke(terrain.edge);
        hex.setStrokeWidth(2);
        hex.setStrokeLineJoin(StrokeLineJoin.ROUND);
        getChildren().add(hex);

        drawTerrainGlyph(cx, cy, terrain);

        if (number != null) {
            Circle token = new Circle(cx, cy, 14);
            boolean hot = number == 6 || number == 8;
            token.setFill(Color.web("#fff3d6"));
            token.setStroke(hot ? Color.web("#b9281b") : Color.web("#5a3a1c"));
            token.setStrokeWidth(1.5);
            token.setEffect(new DropShadow(4, Color.color(0, 0, 0, 0.45)));
            getChildren().add(token);

            Text num = new Text(String.valueOf(number));
            num.setFont(Font.font("Georgia", FontWeight.BOLD, hot ? 16 : 14));
            num.setFill(hot ? Color.web("#b9281b") : Color.web("#2a1a05"));
            num.setTextAlignment(TextAlignment.CENTER);
            num.setX(cx - num.getLayoutBounds().getWidth() / 2);
            num.setY(cy + 5);
            getChildren().add(num);
        } else {
            Circle cage = new Circle(cx, cy, 14);
            cage.setFill(Color.web("#7e3fb8"));
            cage.setStroke(Color.web("#1a1108"));
            cage.setStrokeWidth(2);
            cage.setEffect(new DropShadow(6, Color.color(0, 0, 0, 0.5)));
            getChildren().add(cage);
            Text label = new Text("N");
            label.setFont(Font.font("Georgia", FontWeight.BOLD, 14));
            label.setFill(Color.WHITE);
            label.setX(cx - 4);
            label.setY(cy + 5);
            getChildren().add(label);
        }

        Text terrainLabel = new Text(terrain.label);
        terrainLabel.setFont(Font.font("Inter", FontWeight.BOLD, 7));
        terrainLabel.setFill(Color.web("#2a1a05", 0.7));
        double tw = terrainLabel.getLayoutBounds().getWidth();
        terrainLabel.setX(cx - tw / 2);
        terrainLabel.setY(cy + 24);
        getChildren().add(terrainLabel);
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

        drawPipe(cx - HEX_W / 2, cy - HEX_SIZE,       cx,             cy - HEX_SIZE * 1.3, PColor.RED);
        drawPipe(cx + HEX_W / 2, cy - HEX_SIZE * 0.8, cx + HEX_W,     cy - HEX_SIZE,       PColor.RED);
        drawPipe(cx,             cy + HEX_SIZE * 0.5, cx + HEX_W / 2, cy + HEX_SIZE * 0.8, PColor.BLUE);
        drawPipe(cx - HEX_W,     cy + HEX_SIZE * 0.6, cx - HEX_W * 0.5, cy + HEX_SIZE,     PColor.GOLD);
        drawPipe(cx + HEX_W * 0.5, cy + HEX_SIZE * 1.3, cx + HEX_W,   cy + HEX_SIZE * 1.1, PColor.GOLD);

        drawWatchPost(cx + HEX_W * 0.5,  cy - HEX_SIZE * 1.5, PColor.RED);
        drawWatchPost(cx - HEX_W * 0.5,  cy + HEX_SIZE * 0.5, PColor.BLUE);
        drawWatchPost(cx + HEX_W,        cy + HEX_SIZE,       PColor.GOLD);
        drawWatchPost(cx - HEX_W * 1.5,  cy,                  PColor.WHITE);

        drawLab(cx + HEX_W * 0.5, cy - HEX_SIZE * 0.6, PColor.RED);
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

    private enum Terrain {
        HUTAN  ("#3a9648", "#175a25", "Hutan"),
        BUKIT  ("#d56a3a", "#7a2f12", "Bukit"),
        LADANG ("#ffd864", "#c2901c", "Ladang"),
        TAMBANG("#9a9a92", "#4a4a44", "Tambang"),
        KEBUN  ("#6cbf48", "#2f6e1f", "Kebun Pisang"),
        GURUN  ("#f1d588", "#b58b3a", "Gurun");

        final Color fill, edge;
        final String label;
        Terrain(String fill, String edge, String label) {
            this.fill = Color.web(fill);
            this.edge = Color.web(edge);
            this.label = label;
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
