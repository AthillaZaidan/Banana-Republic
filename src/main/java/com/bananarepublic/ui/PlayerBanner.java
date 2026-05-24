package com.bananarepublic.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public final class PlayerBanner extends HBox {
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
        StackPane.setAlignment(vpBadge, Pos.TOP_LEFT);
        vpBadge.setTranslateX(-8); vpBadge.setTranslateY(-8);
        StackPane portraitStack = new StackPane(portrait, vpBadge);

        VBox body = new VBox();
        body.setMinWidth(170);

        Label nameLabel = new Label(name + (you ? " (You)" : ""));
        nameLabel.getStyleClass().add("player-banner__name");
        nameLabel.setMaxWidth(Double.MAX_VALUE);

        HBox stats = new HBox(10);
        stats.getStyleClass().add("player-banner__stats");
        stats.setAlignment(Pos.CENTER_LEFT);
        stats.getChildren().add(makeStat("🃏", cards));
        stats.getChildren().add(makeStat("⚔", knights));
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        stats.getChildren().add(spacer);

        body.getChildren().addAll(nameLabel, stats);
        getChildren().addAll(portraitStack, body);
    }

    private Label makeStat(String icon, int value) {
        Label l = new Label(icon + " " + value);
        l.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: -ink-on-dark;");
        return l;
    }
}
