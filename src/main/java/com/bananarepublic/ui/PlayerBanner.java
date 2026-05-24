package com.bananarepublic.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.StrokeType;

import java.util.Map;

public final class PlayerBanner extends HBox {

    private static final Map<String, Color[]> PALETTE = Map.of(
        "red",   new Color[]{Color.web("#e64b3f"), Color.web("#962820")},
        "blue",  new Color[]{Color.web("#2c6db5"), Color.web("#1b4778")},
        "gold",  new Color[]{Color.web("#f5c93a"), Color.web("#a07c14")},
        "white", new Color[]{Color.web("#efe6cc"), Color.web("#b8aa86")}
    );
    private static final Color OCEAN_DEEP = Color.web("#0e3a5a");

    public PlayerBanner(String name, String color, int vp, int cards, int knights,
                        boolean active, boolean you) {
        getStyleClass().addAll("player-banner", "pc-" + color);
        if (active) getStyleClass().add("is-active");
        setAlignment(Pos.CENTER_LEFT);

        StackPane portrait = new StackPane();
        portrait.getStyleClass().add("player-banner__portrait");
        Label initial = new Label(String.valueOf(name.charAt(0)));
        initial.setStyle("-fx-font-family: Georgia; -fx-font-weight: 900; -fx-font-size: 24px;"
            + " -fx-text-fill: #5a3a1c;");
        portrait.getChildren().add(initial);

        Label vpBadge = new Label(String.valueOf(vp));
        vpBadge.getStyleClass().add("player-banner__vp");
        vpBadge.setTranslateX(-8); vpBadge.setTranslateY(-8);
        StackPane portraitStack = new StackPane(portrait, vpBadge);
        StackPane.setAlignment(vpBadge, Pos.TOP_LEFT);

        VBox body = new VBox();
        body.setMinWidth(170);

        HBox nameRow = new HBox();
        nameRow.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label(name + (you ? " (You)" : ""));
        nameLabel.getStyleClass().add("player-banner__name");
        nameLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(nameLabel, Priority.ALWAYS);

        Polygon tail = makePennantTail(color);

        nameRow.getChildren().addAll(nameLabel, tail);

        HBox stats = new HBox(10);
        stats.getStyleClass().add("player-banner__stats");
        stats.setAlignment(Pos.CENTER_LEFT);
        stats.getChildren().addAll(statLabel("🃏 " + cards), statLabel("⚔ " + knights));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        stats.getChildren().add(spacer);

        body.getChildren().addAll(nameRow, stats);
        getChildren().addAll(portraitStack, body);
    }

    private static Polygon makePennantTail(String color) {
        Color[] pair = PALETTE.getOrDefault(color, PALETTE.get("red"));
        Polygon tail = new Polygon(
            0,  0,
            16, 0,
            10, 13,
            16, 26,
            0,  26
        );
        tail.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, pair[0]), new Stop(1, pair[1])));
        tail.setStroke(OCEAN_DEEP);
        tail.setStrokeWidth(2);
        tail.setStrokeType(StrokeType.INSIDE);
        tail.setEffect(new DropShadow(2, Color.color(0, 0, 0, 0.2)));
        return tail;
    }

    private static Label statLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: -ink-on-dark;");
        return l;
    }
}
