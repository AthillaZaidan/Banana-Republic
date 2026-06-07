package com.bananarepublic.ui;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

public final class ResourceIcons {

    public enum Kind { WOOD, BRICK, WHEAT, ORE, BANANA }

    private ResourceIcons() {}

    public static Group of(Kind kind) {
        return switch (kind) {
            case WOOD   -> wood();
            case BRICK  -> brick();
            case WHEAT  -> wheat();
            case ORE    -> ore();
            case BANANA -> banana();
        };
    }

    public static Group wood() {
        Group g = new Group();
        Line trunk = new Line(11, 22, 11, 14);
        trunk.setStroke(Color.web("#5a3a1c"));
        trunk.setStrokeWidth(2.5);
        trunk.setStrokeLineCap(StrokeLineCap.ROUND);
        g.getChildren().add(trunk);

        Polygon leaves = new Polygon(
            11, 0,
            21, 14,
            14, 14,
            17, 18,
            11, 18,
            5,  18,
            8,  14,
            1,  14
        );
        leaves.setFill(Color.web("#2a8a3a"));
        leaves.setStroke(Color.web("#0d3a1a"));
        leaves.setStrokeWidth(1);
        leaves.setStrokeLineJoin(StrokeLineJoin.ROUND);
        g.getChildren().add(leaves);
        return g;
    }

    public static Group brick() {
        Group g = new Group();
        for (int row = 0; row < 3; row++) {
            double y = 4 + row * 6;
            double offset = (row % 2 == 0) ? 0 : 6;
            for (int col = 0; col < 2; col++) {
                Rectangle b = new Rectangle(offset + col * 12, y, 10, 5);
                b.setArcWidth(1.5); b.setArcHeight(1.5);
                b.setFill(Color.web("#c4623a"));
                b.setStroke(Color.web("#7a2f12"));
                b.setStrokeWidth(0.8);
                g.getChildren().add(b);
            }
        }
        return g;
    }

    public static Group wheat() {
        Group g = new Group();
        for (int i = 0; i < 3; i++) {
            double x = 4 + i * 7;
            Line stem = new Line(x, 22, x, 8);
            stem.setStroke(Color.web("#8a5a14"));
            stem.setStrokeWidth(1.2);
            g.getChildren().add(stem);

            Ellipse head = new Ellipse(x, 6, 3, 4);
            head.setFill(Color.web("#e2b430"));
            head.setStroke(Color.web("#8a5a14"));
            head.setStrokeWidth(0.6);
            g.getChildren().add(head);

            Line whiskerL = new Line(x, 10, x - 3, 6);
            whiskerL.setStroke(Color.web("#a07c14"));
            whiskerL.setStrokeWidth(0.8);
            g.getChildren().add(whiskerL);

            Line whiskerR = new Line(x, 10, x + 3, 6);
            whiskerR.setStroke(Color.web("#a07c14"));
            whiskerR.setStrokeWidth(0.8);
            g.getChildren().add(whiskerR);
        }
        return g;
    }

    public static Group ore() {
        Group g = new Group();
        Polygon rock = new Polygon(
            2,  18,
            8,  6,
            14, 10,
            20, 4,
            22, 18
        );
        rock.setFill(Color.web("#7e7e76"));
        rock.setStroke(Color.web("#3a3a34"));
        rock.setStrokeWidth(1.2);
        rock.setStrokeLineJoin(StrokeLineJoin.ROUND);
        g.getChildren().add(rock);

        Polygon highlight = new Polygon(
            8,  8,
            12, 12,
            10, 14
        );
        highlight.setFill(Color.web("#9a9a92"));
        g.getChildren().add(highlight);
        return g;
    }

    public static Group banana() {
        Group g = new Group();
        for (int i = 0; i < 3; i++) {
            Ellipse fruit = new Ellipse(11 + (i - 1) * 4, 12 + i, 7, 3);
            fruit.setFill(Color.web("#ffd23d"));
            fruit.setStroke(Color.web("#8a5a0a"));
            fruit.setStrokeWidth(0.9);
            fruit.setRotate(-30 + i * 18);
            g.getChildren().add(fruit);
        }
        Line stem = new Line(11, 6, 14, 4);
        stem.setStroke(Color.web("#5a3a1c"));
        stem.setStrokeWidth(1.4);
        stem.setStrokeLineCap(StrokeLineCap.ROUND);
        g.getChildren().add(stem);
        return g;
    }
}
