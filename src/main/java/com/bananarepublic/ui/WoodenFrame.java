package com.bananarepublic.ui;

import javafx.scene.Group;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

public final class WoodenFrame {
    private WoodenFrame() {}

    public static Group build(double width, double height) {
        Region top = new Region();
        top.getStyleClass().addAll("wood-frame-edge", "wood-frame-edge-h");
        top.setPrefWidth(width); top.setLayoutY(0);

        Region bottom = new Region();
        bottom.getStyleClass().addAll("wood-frame-edge", "wood-frame-edge-h");
        bottom.setPrefWidth(width); bottom.setLayoutY(height - 22);

        Region left = new Region();
        left.getStyleClass().addAll("wood-frame-edge", "wood-frame-edge-v");
        left.setPrefHeight(height);

        Region right = new Region();
        right.getStyleClass().addAll("wood-frame-edge", "wood-frame-edge-v");
        right.setPrefHeight(height); right.setLayoutX(width - 22);

        Region rTL = rivet(4, 4);
        Region rTR = rivet(width - 18, 4);
        Region rBL = rivet(4, height - 18);
        Region rBR = rivet(width - 18, height - 18);

        Group g = new Group(top, bottom, left, right, rTL, rTR, rBL, rBR);
        g.setMouseTransparent(true);
        return g;
    }

    private static Region rivet(double x, double y) {
        Region r = new Region();
        r.getStyleClass().add("wood-frame-rivet");
        r.setLayoutX(x); r.setLayoutY(y);
        return r;
    }
}
