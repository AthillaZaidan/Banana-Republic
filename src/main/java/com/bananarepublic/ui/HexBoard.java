package com.bananarepublic.ui;

import javafx.scene.Group;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Rotate;

import java.util.List;

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

    public HexBoard(double width, double height) {
        setPrefSize(width, height);
        setMinSize(width, height);
        setMaxSize(width, height);
        getTransforms().add(new Rotate(14, width / 2, height / 2, 0, Rotate.X_AXIS));
        buildIsland(width, height);
        buildHexes(width, height);
    }

    private void buildIsland(double w, double h) {
        double cx = w / 2;
        double cy = h / 2;
        javafx.scene.shape.Ellipse ring = new javafx.scene.shape.Ellipse(cx, cy + 8, 380, 320);
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
        HUTAN ("#3a9648", "#175a25"),
        BUKIT ("#d56a3a", "#7a2f12"),
        LADANG("#ffd864", "#c2901c"),
        TAMBANG("#9a9a92", "#4a4a44"),
        KEBUN ("#6cbf48", "#2f6e1f"),
        GURUN ("#f1d588", "#b58b3a");

        final Color fill, edge;
        Terrain(String fill, String edge) {
            this.fill = Color.web(fill);
            this.edge = Color.web(edge);
        }
    }
}
