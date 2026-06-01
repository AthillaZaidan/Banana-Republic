package com.bananarepublic.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public final class DicePips {

    private DicePips() {}

    public static void render(Pane pane, int value, double size) {
        pane.getChildren().clear();
        pane.setMinSize(size, size);
        pane.setPrefSize(size, size);
        pane.setMaxSize(size, size);
        pane.setMouseTransparent(true);

        StackPane overlay = new StackPane();
        overlay.setMinSize(size, size);
        overlay.setPrefSize(size, size);
        overlay.setMaxSize(size, size);
        overlay.setAlignment(Pos.CENTER);

        Label numLabel = new Label(String.valueOf(value));
        double fontSize = size * 0.58;
        Font font = Font.font("Gemunu Libre", FontWeight.EXTRA_BOLD, fontSize);
        numLabel.setFont(font);
        numLabel.setStyle(
            "-fx-text-fill: #1a2e0a;"
            + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 2, 0.4, 0, 1);"
        );
        numLabel.setTranslateY(-size * 0.04);

        overlay.getChildren().add(numLabel);
        pane.getChildren().add(overlay);
    }

    public static Pane createGraphic(int value, double size) {
        Pane pane = new Pane();
        render(pane, value, size);
        return pane;
    }
}
