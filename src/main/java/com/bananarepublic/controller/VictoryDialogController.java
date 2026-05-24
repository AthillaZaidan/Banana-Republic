package com.bananarepublic.controller;

import com.bananarepublic.ui.Navigator;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;

public class VictoryDialogController {
    private record Card(String kind, String name, String type, String value) {}

    @FXML private StackPane root;
    @FXML private VBox secretList;

    @FXML
    public void initialize() {
        List<Card> cards = List.of(
            new Card("vp",     "Library",       "Victory Point Card", "+1 VP"),
            new Card("vp",     "Cathedral",     "Victory Point Card", "+1 VP"),
            new Card("knight", "Knight (used)", "Knight Card",        "PLAYED")
        );
        for (Card c : cards) {
            secretList.getChildren().add(buildRow(c));
        }
    }

    private HBox buildRow(Card c) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-padding: 10 12; -fx-background-radius: 10;"
            + " -fx-background-color: rgba(255,255,255,0.55);"
            + " -fx-border-color: -parchment-line; -fx-border-radius: 10; -fx-border-width: 1;");

        StackPane swatch = new StackPane();
        swatch.setMinSize(28, 28); swatch.setMaxSize(28, 28);
        swatch.setStyle("-fx-background-radius: 6;"
            + (c.kind().equals("vp")
                ? "-fx-background-color: -gold-1;"
                : "-fx-background-color: #7e3fb8;"));
        Label icon = new Label(c.kind().equals("vp") ? "📜" : "⚔");
        icon.setStyle("-fx-text-fill: white;");
        swatch.getChildren().add(icon);

        VBox info = new VBox(2);
        Label name = new Label(c.name());
        name.setStyle("-fx-font-weight: 800; -fx-font-size: 13px;");
        Label type = new Label(c.type());
        type.setStyle("-fx-font-size: 11px; -fx-text-fill: -ink-mute;");
        info.getChildren().addAll(name, type);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label val = new Label(c.value());
        val.setStyle("-fx-padding: 4 10; -fx-background-radius: 999;"
            + " -fx-font-size: 11px; -fx-font-weight: 800;"
            + (c.kind().equals("vp")
                ? "-fx-background-color: -gold-1; -fx-text-fill: -gold-deep;"
                : "-fx-background-color: rgba(0,0,0,0.08); -fx-text-fill: -ink-mute;"));

        row.getChildren().addAll(swatch, info, val);
        return row;
    }

    @FXML
    private void onConfirm() {
        System.out.println("[Victory] confirm");
        Navigator.closeOverlay(root);
        Navigator.goTo("/fxml/game_result.fxml");
    }

    @FXML
    private void onClose() { close(); }

    private void close() {
        Navigator.closeOverlay(root);
    }
}
