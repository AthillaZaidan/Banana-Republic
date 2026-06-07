package com.bananarepublic.ui;

import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public final class DicePips {

    private DicePips() {}

    public static void render(Pane pane, int value, double size) {
        pane.getChildren().clear();
        pane.setMinSize(size, size);
        pane.setPrefSize(size, size);
        pane.setMaxSize(size, size);
        pane.setMouseTransparent(true);

        double left = size * 0.28;
        double center = size * 0.50;
        double right = size * 0.72;
        double top = size * 0.28;
        double middle = size * 0.50;
        double bottom = size * 0.72;
        double radius = Math.max(4, size * 0.085);

        switch (value) {
            case 1 -> addPip(pane, center, middle, radius);
            case 2 -> {
                addPip(pane, left, top, radius);
                addPip(pane, right, bottom, radius);
            }
            case 3 -> {
                addPip(pane, left, top, radius);
                addPip(pane, center, middle, radius);
                addPip(pane, right, bottom, radius);
            }
            case 4 -> {
                addPip(pane, left, top, radius);
                addPip(pane, right, top, radius);
                addPip(pane, left, bottom, radius);
                addPip(pane, right, bottom, radius);
            }
            case 5 -> {
                addPip(pane, left, top, radius);
                addPip(pane, right, top, radius);
                addPip(pane, center, middle, radius);
                addPip(pane, left, bottom, radius);
                addPip(pane, right, bottom, radius);
            }
            case 6 -> {
                addPip(pane, left, top, radius);
                addPip(pane, right, top, radius);
                addPip(pane, left, middle, radius);
                addPip(pane, right, middle, radius);
                addPip(pane, left, bottom, radius);
                addPip(pane, right, bottom, radius);
            }
            default -> addPip(pane, center, middle, radius);
        }
    }

    public static Pane createGraphic(int value, double size) {
        Pane pane = new Pane();
        render(pane, value, size);
        return pane;
    }

    private static void addPip(Pane pane, double x, double y, double radius) {
        Circle pip = new Circle(radius);
        pip.setCenterX(x);
        pip.setCenterY(y);
        pip.setFill(Color.web("#2b180f"));
        pip.setEffect(new DropShadow(radius * 0.35, Color.rgb(0, 0, 0, 0.18)));
        pane.getChildren().add(pip);
    }
}
