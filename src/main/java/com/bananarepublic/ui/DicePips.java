package com.bananarepublic.ui;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;

public final class DicePips {
    private static final Paint DEFAULT_PIP_COLOR = Paint.valueOf("#201408");

    private DicePips() {
    }

    public static void render(Pane pane, int value, double size) {
        render(pane, value, size, DEFAULT_PIP_COLOR);
    }

    public static void render(Pane pane, int value, double size, Paint pipColor) {
        pane.getChildren().clear();
        pane.setMinSize(size, size);
        pane.setPrefSize(size, size);
        pane.setMaxSize(size, size);
        pane.setMouseTransparent(true);

        double left = size * 0.22;
        double center = size * 0.50;
        double right = size * 0.78;
        double top = size * 0.22;
        double middle = size * 0.50;
        double bottom = size * 0.78;
        double radius = Math.max(4.0, size * 0.11);

        switch (value) {
            case 1 -> addPip(pane, center, middle, radius, pipColor);
            case 2 -> {
                addPip(pane, left, top, radius, pipColor);
                addPip(pane, right, bottom, radius, pipColor);
            }
            case 3 -> {
                addPip(pane, left, top, radius, pipColor);
                addPip(pane, center, middle, radius, pipColor);
                addPip(pane, right, bottom, radius, pipColor);
            }
            case 4 -> {
                addPip(pane, left, top, radius, pipColor);
                addPip(pane, right, top, radius, pipColor);
                addPip(pane, left, bottom, radius, pipColor);
                addPip(pane, right, bottom, radius, pipColor);
            }
            case 5 -> {
                addPip(pane, left, top, radius, pipColor);
                addPip(pane, right, top, radius, pipColor);
                addPip(pane, center, middle, radius, pipColor);
                addPip(pane, left, bottom, radius, pipColor);
                addPip(pane, right, bottom, radius, pipColor);
            }
            case 6 -> {
                addPip(pane, left, top, radius, pipColor);
                addPip(pane, right, top, radius, pipColor);
                addPip(pane, left, middle, radius, pipColor);
                addPip(pane, right, middle, radius, pipColor);
                addPip(pane, left, bottom, radius, pipColor);
                addPip(pane, right, bottom, radius, pipColor);
            }
            default -> addPip(pane, center, middle, radius, pipColor);
        }
    }

    public static Pane createGraphic(int value, double size) {
        Pane pane = new Pane();
        render(pane, value, size);
        return pane;
    }

    private static void addPip(Pane pane, double centerX, double centerY, double radius, Paint pipColor) {
        Circle pip = new Circle(centerX, centerY, radius);
        pip.setFill(pipColor);
        pane.getChildren().add(pip);
    }
}
