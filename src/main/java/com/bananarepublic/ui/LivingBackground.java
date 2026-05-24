package com.bananarepublic.ui;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.Group;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Path;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.QuadCurveTo;
import javafx.scene.shape.StrokeLineCap;
import javafx.util.Duration;

public final class LivingBackground {

    public enum Variant { PARCHMENT, OCEAN, SLATE }

    private LivingBackground() {}

    public static void attach(Pane layer, Variant variant) {
        if (layer == null) return;
        layer.setMouseTransparent(true);

        layer.getChildren().add(buildCloud(120, 60, 1.0,  0.85, 80));
        layer.getChildren().add(buildCloud(420, 120, 0.7, 0.65, 56));
        layer.getChildren().add(buildCloud(700, 200, 0.55, 0.5, 42));

        Group gull = buildGull(variant == Variant.SLATE
            ? Color.color(1, 1, 1, 0.4) : Color.WHITE);
        layer.getChildren().add(gull);
        animateGull(gull, layer);

        if (variant != Variant.SLATE) {
            Group ship = buildShip();
            ship.setLayoutX(160);
            ship.setLayoutY(520);
            layer.getChildren().add(ship);
            animateShipBob(ship);
        }
    }

    private static Ellipse buildCloud(double x, double y, double scale,
                                      double opacity, int duration) {
        Ellipse cloud = new Ellipse(110 * scale, 32 * scale);
        cloud.setLayoutX(x);
        cloud.setLayoutY(y);
        cloud.setOpacity(opacity);
        RadialGradient grad = new RadialGradient(
            0, 0, 0.5, 0.5, 1.0, true, javafx.scene.paint.CycleMethod.NO_CYCLE,
            new Stop(0, Color.color(1, 1, 1, 0.95)),
            new Stop(1, Color.color(1, 1, 1, 0)));
        cloud.setFill(grad);
        cloud.setEffect(new GaussianBlur(2));
        animateCloud(cloud, duration);
        return cloud;
    }

    private static void animateCloud(Ellipse cloud, int seconds) {
        double startX = cloud.getLayoutX();
        Timeline tl = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(cloud.translateXProperty(), -260, Interpolator.LINEAR)),
            new KeyFrame(Duration.seconds(seconds),
                new KeyValue(cloud.translateXProperty(), 1500 - startX, Interpolator.LINEAR)));
        tl.setCycleCount(Animation.INDEFINITE);
        tl.play();
    }

    private static Group buildGull(Color color) {
        Path wing = new Path(
            new MoveTo(2, 18),
            new QuadCurveTo(12, 2, 24, 12),
            new QuadCurveTo(36, 2, 46, 18));
        wing.setStroke(color);
        wing.setStrokeWidth(2.5);
        wing.setStrokeLineCap(StrokeLineCap.ROUND);
        wing.setFill(null);
        return new Group(wing);
    }

    private static void animateGull(Group gull, Pane layer) {
        gull.setLayoutY(80);
        Timeline tl = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(gull.translateXProperty(), -100),
                new KeyValue(gull.opacityProperty(), 0)),
            new KeyFrame(Duration.seconds(2),
                new KeyValue(gull.opacityProperty(), 0.9)),
            new KeyFrame(Duration.seconds(26),
                new KeyValue(gull.opacityProperty(), 0.9)),
            new KeyFrame(Duration.seconds(28),
                new KeyValue(gull.translateXProperty(), 1400),
                new KeyValue(gull.translateYProperty(), -40),
                new KeyValue(gull.opacityProperty(), 0)));
        tl.setCycleCount(Animation.INDEFINITE);
        tl.play();
    }

    private static Group buildShip() {
        Path hull = new Path(
            new MoveTo(4, 30),
            new javafx.scene.shape.LineTo(56, 30),
            new javafx.scene.shape.LineTo(50, 42),
            new javafx.scene.shape.LineTo(10, 42),
            new javafx.scene.shape.LineTo(4, 30));
        hull.setFill(Color.web("#7c4a26"));
        hull.setStroke(Color.web("#3a1d0a"));
        hull.setStrokeWidth(1.4);

        javafx.scene.shape.Rectangle mast = new javafx.scene.shape.Rectangle(28, 6, 2, 24);
        mast.setFill(Color.web("#3a1d0a"));

        Path sailR = new Path(
            new MoveTo(30, 8),
            new javafx.scene.shape.LineTo(48, 22),
            new javafx.scene.shape.LineTo(30, 22),
            new javafx.scene.shape.LineTo(30, 8));
        sailR.setFill(Color.web("#fff5d6"));
        sailR.setStroke(Color.web("#3a1d0a"));
        sailR.setStrokeWidth(1.2);

        Path sailL = new Path(
            new MoveTo(30, 8),
            new javafx.scene.shape.LineTo(14, 22),
            new javafx.scene.shape.LineTo(30, 22),
            new javafx.scene.shape.LineTo(30, 8));
        sailL.setFill(Color.web("#fff5d6"));
        sailL.setStroke(Color.web("#3a1d0a"));
        sailL.setStrokeWidth(1.2);

        Circle flag = new Circle(30, 5, 1.5, Color.web("#e64b3f"));
        return new Group(hull, mast, sailR, sailL, flag);
    }

    private static void animateShipBob(Group ship) {
        Timeline tl = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(ship.translateYProperty(), 0),
                new KeyValue(ship.rotateProperty(), -2)),
            new KeyFrame(Duration.seconds(2.25),
                new KeyValue(ship.translateYProperty(), -4),
                new KeyValue(ship.rotateProperty(), 2)),
            new KeyFrame(Duration.seconds(4.5),
                new KeyValue(ship.translateYProperty(), 0),
                new KeyValue(ship.rotateProperty(), -2)));
        tl.setCycleCount(Animation.INDEFINITE);
        tl.play();
    }

}
