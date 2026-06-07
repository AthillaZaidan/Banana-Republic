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
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.CubicCurveTo;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.QuadCurveTo;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.util.Duration;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class LivingBackground {

    public enum Variant { PARCHMENT, OCEAN, SLATE }

    private static volatile boolean animationsEnabled = true;
    private static final List<Timeline> running = new CopyOnWriteArrayList<>();

    private LivingBackground() {}

    public static void setAnimationsEnabled(boolean enabled) {
        animationsEnabled = enabled;
        for (Timeline tl : running) {
            if (enabled) tl.play(); else tl.pause();
        }
    }

    public static boolean isAnimationsEnabled() { return animationsEnabled; }

    // Called for non-game screens (lobby, menu) — fixed-layer, screen-space
    public static void attach(Pane layer, Variant variant) {
        if (layer == null) return;
        layer.setMouseTransparent(true);
        layer.getStyleClass().add("animated-bg");

        layer.getChildren().add(buildCloud(120,  60, 1.00, 0.82, 80));
        layer.getChildren().add(buildCloud(500, 100, 0.72, 0.62, 57));
        layer.getChildren().add(buildCloud(820, 180, 0.55, 0.48, 44));

        Group gull = buildGull(variant == Variant.SLATE
                ? Color.color(1, 1, 1, 0.4) : Color.WHITE);
        layer.getChildren().add(gull);
        animateGull(gull, 1280);

        if (variant != Variant.SLATE) {
            Group ship = buildShip();
            ship.setLayoutX(160);
            ship.setLayoutY(520);
            layer.getChildren().add(ship);
            animateShipBob(ship);
        }
    }

    // Called for the game screen — elements live inside boardCanvas (world-space)
    // canvasW/canvasH is BOARD_DESIGN_W / BOARD_DESIGN_H (900 × 780)
    public static void attachToCanvas(Group boardCanvas, double canvasW, double canvasH) {
        // ── Ocean background image ──────────────────────────────────
        // Loaded in GameController and passed in as first child — nothing to do here.

        // ── Clouds spread across a wide world above the board ───────
        // x range: -400 … canvasW+400, y range: -320 … -80
        // Each cloud drifts left→right then wraps; they start staggered.
        Group c1 = makeCanvasCloud(-300, -260, 1.10, 0.78);
        Group c2 = makeCanvasCloud( 200, -200, 0.82, 0.64);
        Group c3 = makeCanvasCloud( 600, -310, 1.30, 0.55);
        Group c4 = makeCanvasCloud(-100, -140, 0.65, 0.50);
        Group c5 = makeCanvasCloud( 850, -230, 0.95, 0.70);

        boardCanvas.getChildren().addAll(c1, c2, c3, c4, c5);
        animateCloudCanvas(c1, -400, canvasW + 400, 90);
        animateCloudCanvas(c2,  200, canvasW + 500, 65);
        animateCloudCanvas(c3, -500, canvasW + 300, 110);
        animateCloudCanvas(c4, -100, canvasW + 400, 52);
        animateCloudCanvas(c5,  850, canvasW + 600, 78);

        // ── Gulls ───────────────────────────────────────────────────
        Group gull1 = buildGull(Color.WHITE);
        gull1.setLayoutY(-120);
        boardCanvas.getChildren().add(gull1);
        animateGull(gull1, canvasW + 300);

        Group gull2 = buildGull(Color.color(1, 1, 1, 0.7));
        gull2.setLayoutY(-80);
        boardCanvas.getChildren().add(gull2);
        animateGullDelayed(gull2, canvasW + 300, 12);

        // ── Ships ───────────────────────────────────────────────────
        Group ship1 = buildShip();
        ship1.setLayoutX(-180);
        ship1.setLayoutY(canvasH - 80);
        boardCanvas.getChildren().add(ship1);
        animateShipBob(ship1);
        animateShipDrift(ship1, -220, canvasW + 200, 140);

        Group ship2 = buildShip();
        ship2.setScaleX(0.72);
        ship2.setScaleY(0.72);
        ship2.setLayoutX(canvasW + 80);
        ship2.setLayoutY(canvasH - 40);
        boardCanvas.getChildren().add(ship2);
        animateShipBob(ship2);
        animateShipDrift(ship2, canvasW + 100, -250, 200);
    }

    // ── Cloud builders ─────────────────────────────────────────────────────────

    // Screen-space cloud — auto-animates with horizontal drift
    private static Group buildCloud(double x, double y, double scale,
                                    double opacity, int durationSec) {
        Group g = buildCloudShape(scale, opacity);
        g.setLayoutX(x);
        g.setLayoutY(y);
        animateCloud(g, durationSec);
        return g;
    }

    // Canvas-space cloud — no built-in animation; caller drives with animateCloudCanvas()
    private static Group makeCanvasCloud(double x, double y, double scale, double opacity) {
        Group g = buildCloudShape(scale, opacity);
        g.setLayoutX(x);
        g.setLayoutY(y);
        return g;
    }

    private static Group buildCloudShape(double scale, double opacity) {
        Group g = new Group();

        // Each cloud = several overlapping ellipses at different offsets → puffy cumulus look
        double[][] blobs = {
            // { relX, relY, rx, ry, blur, alpha }
            {   0,  18, 80*scale, 28*scale, 6,  0.88 },  // wide base
            {  30,   4, 52*scale, 38*scale, 5,  0.82 },  // main puff centre-right
            { -25,   6, 44*scale, 34*scale, 5,  0.78 },  // main puff centre-left
            {  60,  16, 36*scale, 24*scale, 4,  0.68 },  // right shoulder
            { -50,  16, 34*scale, 22*scale, 4,  0.64 },  // left shoulder
            {  15,  -6, 30*scale, 22*scale, 4,  0.60 },  // top centre dome
        };

        for (double[] b : blobs) {
            Ellipse e = new Ellipse(b[2], b[3]);
            e.setTranslateX(b[0]);
            e.setTranslateY(b[1]);
            double a = b[5];
            e.setFill(new RadialGradient(
                    0, 0, 0.45, 0.4, 0.9, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.color(1, 1, 1, Math.min(1, a + 0.05))),
                    new Stop(0.55, Color.color(1, 1, 1, a * 0.8)),
                    new Stop(1, Color.color(1, 1, 1, 0))));
            e.setEffect(new GaussianBlur(b[4]));
            g.getChildren().add(e);
        }

        g.setOpacity(opacity);
        return g;
    }

    // Screen-space cloud animation (used in attach())
    private static void animateCloud(Group cloud, int seconds) {
        double startX = cloud.getLayoutX();
        Timeline tl = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(cloud.translateXProperty(), -300, Interpolator.LINEAR)),
                new KeyFrame(Duration.seconds(seconds),
                        new KeyValue(cloud.translateXProperty(), 1600 - startX, Interpolator.LINEAR)));
        tl.setCycleCount(Animation.INDEFINITE);
        // subtle vertical drift
        Timeline vDrift = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(cloud.translateYProperty(), 0, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.seconds(seconds * 0.4),
                        new KeyValue(cloud.translateYProperty(), 18, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.seconds(seconds * 0.8),
                        new KeyValue(cloud.translateYProperty(), -10, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.seconds(seconds),
                        new KeyValue(cloud.translateYProperty(), 0, Interpolator.EASE_BOTH)));
        vDrift.setCycleCount(Animation.INDEFINITE);
        track(tl);
        track(vDrift);
    }

    // World-space (canvas) cloud — drifts from startX to endX then wraps
    private static void animateCloudCanvas(Group cloud, double startX, double endX, int seconds) {
        double originY = cloud.getLayoutY();
        cloud.setLayoutX(startX);

        Timeline hDrift = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(cloud.layoutXProperty(), startX, Interpolator.LINEAR)),
                new KeyFrame(Duration.seconds(seconds),
                        new KeyValue(cloud.layoutXProperty(), endX,   Interpolator.LINEAR)));
        hDrift.setCycleCount(Animation.INDEFINITE);

        // gentle up-down oscillation
        double amplitude = 14 + Math.random() * 18;
        double period    = 18 + Math.random() * 22;
        Timeline vDrift = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(cloud.translateYProperty(), 0,           Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.seconds(period * 0.5),
                        new KeyValue(cloud.translateYProperty(), amplitude,   Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.seconds(period),
                        new KeyValue(cloud.translateYProperty(), 0,           Interpolator.EASE_BOTH)));
        vDrift.setCycleCount(Animation.INDEFINITE);

        // subtle scale breathe
        double breatheAmt = 0.04 + Math.random() * 0.04;
        Timeline breathe = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(cloud.scaleXProperty(), 1.0,              Interpolator.EASE_BOTH),
                        new KeyValue(cloud.scaleYProperty(), 1.0,              Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.seconds(period * 0.6),
                        new KeyValue(cloud.scaleXProperty(), 1 + breatheAmt,   Interpolator.EASE_BOTH),
                        new KeyValue(cloud.scaleYProperty(), 1 + breatheAmt,   Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.seconds(period),
                        new KeyValue(cloud.scaleXProperty(), 1.0,              Interpolator.EASE_BOTH),
                        new KeyValue(cloud.scaleYProperty(), 1.0,              Interpolator.EASE_BOTH)));
        breathe.setCycleCount(Animation.INDEFINITE);

        track(hDrift);
        track(vDrift);
        track(breathe);
    }

    // ── Gull ───────────────────────────────────────────────────────────────────

    private static Group buildGull(Color color) {
        // Two curved wings meeting at body centre — more realistic M-shape
        Path wing = new Path(
                new MoveTo(0, 20),
                new CubicCurveTo(8, 4,  18, 0,  26, 10),
                new CubicCurveTo(34, 0, 44, 4,  52, 20));
        wing.setStroke(color);
        wing.setStrokeWidth(2.2);
        wing.setStrokeLineCap(StrokeLineCap.ROUND);
        wing.setFill(null);

        // Small body dot
        Circle body = new Circle(26, 20, 2.5, color);

        return new Group(wing, body);
    }

    private static void animateGull(Group gull, double sceneWidth) {
        gull.setTranslateX(-100);
        gull.setOpacity(0);
        double cycleSeconds = 24 + Math.random() * 8;
        Timeline tl = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(gull.translateXProperty(), -100,         Interpolator.LINEAR),
                        new KeyValue(gull.opacityProperty(),    0)),
                new KeyFrame(Duration.seconds(2),
                        new KeyValue(gull.opacityProperty(),    0.88,         Interpolator.EASE_OUT)),
                new KeyFrame(Duration.seconds(cycleSeconds - 2),
                        new KeyValue(gull.opacityProperty(),    0.88,         Interpolator.EASE_IN)),
                new KeyFrame(Duration.seconds(cycleSeconds),
                        new KeyValue(gull.translateXProperty(), sceneWidth + 80, Interpolator.LINEAR),
                        new KeyValue(gull.translateYProperty(), -30),
                        new KeyValue(gull.opacityProperty(),    0)));
        tl.setCycleCount(Animation.INDEFINITE);

        // Wing flap — scaleY oscillates (simulates flapping silhouette)
        Timeline flap = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(gull.scaleYProperty(), 1.0,  Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.seconds(0.55),
                        new KeyValue(gull.scaleYProperty(), -0.7, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.seconds(1.1),
                        new KeyValue(gull.scaleYProperty(), 1.0,  Interpolator.EASE_BOTH)));
        flap.setCycleCount(Animation.INDEFINITE);

        track(tl);
        track(flap);
    }

    private static void animateGullDelayed(Group gull, double sceneWidth, double delaySeconds) {
        gull.setTranslateX(-100);
        gull.setOpacity(0);
        double cycleSeconds = 32 + Math.random() * 8;
        Timeline delay = new Timeline(new KeyFrame(Duration.seconds(delaySeconds)));
        delay.setOnFinished(e -> {
            Timeline tl = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(gull.translateXProperty(), -100,            Interpolator.LINEAR),
                            new KeyValue(gull.opacityProperty(),    0)),
                    new KeyFrame(Duration.seconds(2.5),
                            new KeyValue(gull.opacityProperty(),    0.75,            Interpolator.EASE_OUT)),
                    new KeyFrame(Duration.seconds(cycleSeconds - 2),
                            new KeyValue(gull.opacityProperty(),    0.75,            Interpolator.EASE_IN)),
                    new KeyFrame(Duration.seconds(cycleSeconds),
                            new KeyValue(gull.translateXProperty(), sceneWidth + 80, Interpolator.LINEAR),
                            new KeyValue(gull.translateYProperty(), -50),
                            new KeyValue(gull.opacityProperty(),    0)));
            tl.setCycleCount(Animation.INDEFINITE);
            Timeline flap = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(gull.scaleYProperty(), 1.0,  Interpolator.EASE_BOTH)),
                    new KeyFrame(Duration.seconds(0.65),
                            new KeyValue(gull.scaleYProperty(), -0.65, Interpolator.EASE_BOTH)),
                    new KeyFrame(Duration.seconds(1.3),
                            new KeyValue(gull.scaleYProperty(), 1.0,  Interpolator.EASE_BOTH)));
            flap.setCycleCount(Animation.INDEFINITE);
            track(tl);
            track(flap);
        });
        delay.play();
    }

    // ── Ship ───────────────────────────────────────────────────────────────────

    private static Group buildShip() {
        // Hull — wider, more realistic trapezoid with curved bottom
        Path hull = new Path(
                new MoveTo(2,  28),
                new LineTo(66, 28),
                new LineTo(60, 44),
                new QuadCurveTo(34, 52, 8, 44),
                new LineTo(2,  28),
                new ClosePath());
        hull.setFill(Color.web("#6b3c1e"));
        hull.setStroke(Color.web("#2e1508"));
        hull.setStrokeWidth(1.5);

        // Hull shading strip
        Path hullShade = new Path(
                new MoveTo(6,  33),
                new LineTo(62, 33),
                new LineTo(58, 40),
                new LineTo(10, 40),
                new ClosePath());
        hullShade.setFill(Color.color(0, 0, 0, 0.14));
        hullShade.setStroke(null);

        // Water line highlight
        Path waterLine = new Path(
                new MoveTo(4,  28),
                new LineTo(64, 28));
        waterLine.setStroke(Color.color(1, 1, 1, 0.22));
        waterLine.setStrokeWidth(1.2);

        // Mast
        Rectangle mast = new Rectangle(32, 2, 3, 28);
        mast.setFill(Color.web("#2e1508"));
        mast.setStroke(Color.web("#1a0c04"));
        mast.setStrokeWidth(0.6);

        // Boom (horizontal spar)
        Rectangle boom = new Rectangle(20, 22, 27, 2);
        boom.setFill(Color.web("#2e1508"));

        // Main sail — curved using cubic
        Path mainSail = new Path(
                new MoveTo(35, 4),
                new CubicCurveTo(52, 8, 54, 18, 48, 22),
                new LineTo(35, 22),
                new ClosePath());
        mainSail.setFill(Color.web("#f5ecd4"));
        mainSail.setStroke(Color.web("#c8a86a"));
        mainSail.setStrokeWidth(1.0);

        // Main sail — left side
        Path mainSailL = new Path(
                new MoveTo(33, 4),
                new CubicCurveTo(16, 8, 14, 18, 20, 22),
                new LineTo(33, 22),
                new ClosePath());
        mainSailL.setFill(Color.web("#ecdfc0"));
        mainSailL.setStroke(Color.web("#c8a86a"));
        mainSailL.setStrokeWidth(1.0);

        // Top pennant
        Path pennant = new Path(
                new MoveTo(35, 2),
                new LineTo(44, -2),
                new LineTo(35, 6),
                new ClosePath());
        pennant.setFill(Color.web("#e04040"));

        // Cabin / deck box
        Rectangle cabin = new Rectangle(16, 20, 14, 9);
        cabin.setArcWidth(2); cabin.setArcHeight(2);
        cabin.setFill(Color.web("#5a3010"));
        cabin.setStroke(Color.web("#2e1508"));
        cabin.setStrokeWidth(0.8);

        // Porthole
        Circle porthole = new Circle(23, 24, 2.5, Color.web("#a0d8e8"));
        porthole.setStroke(Color.web("#2e1508"));
        porthole.setStrokeWidth(0.8);

        // Rope lines from mast to hull
        Path rope1 = new Path(new MoveTo(34, 4), new LineTo(14, 28));
        rope1.setStroke(Color.color(0.2, 0.1, 0.05, 0.55));
        rope1.setStrokeWidth(0.7);

        Path rope2 = new Path(new MoveTo(34, 4), new LineTo(60, 28));
        rope2.setStroke(Color.color(0.2, 0.1, 0.05, 0.55));
        rope2.setStrokeWidth(0.7);

        return new Group(hull, hullShade, waterLine, rope1, rope2,
                mast, boom, mainSailL, mainSail, pennant, cabin, porthole);
    }

    private static void animateShipBob(Group ship) {
        Timeline tl = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(ship.translateYProperty(), 0,    Interpolator.EASE_BOTH),
                        new KeyValue(ship.rotateProperty(),    -2.5,  Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.seconds(2.2),
                        new KeyValue(ship.translateYProperty(), -5,   Interpolator.EASE_BOTH),
                        new KeyValue(ship.rotateProperty(),     2.5,  Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.seconds(4.4),
                        new KeyValue(ship.translateYProperty(), 0,    Interpolator.EASE_BOTH),
                        new KeyValue(ship.rotateProperty(),    -2.5,  Interpolator.EASE_BOTH)));
        tl.setCycleCount(Animation.INDEFINITE);
        track(tl);
    }

    private static void animateShipDrift(Group ship, double fromX, double toX, int seconds) {
        boolean leftToRight = toX > fromX;
        ship.setScaleX(leftToRight ? 1 : -1);   // mirror ship direction
        ship.setLayoutX(fromX);
        Timeline tl = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(ship.layoutXProperty(), fromX, Interpolator.LINEAR)),
                new KeyFrame(Duration.seconds(seconds),
                        new KeyValue(ship.layoutXProperty(), toX,   Interpolator.LINEAR)));
        tl.setCycleCount(Animation.INDEFINITE);
        track(tl);
    }

    // ── Timeline registry ──────────────────────────────────────────────────────

    private static void track(Timeline tl) {
        running.add(tl);
        if (animationsEnabled) tl.play();
    }
}
