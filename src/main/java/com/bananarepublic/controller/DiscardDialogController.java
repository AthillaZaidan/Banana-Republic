package com.bananarepublic.controller;

import com.bananarepublic.ui.Navigator;
import com.bananarepublic.ui.ResourceIcons;
import com.bananarepublic.ui.Stepper;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public class DiscardDialogController {
    private record Holding(ResourceIcons.Kind kind, String label, int hold) {}
    private static final List<Holding> HOLDINGS = List.of(
        new Holding(ResourceIcons.Kind.WOOD,   "WOOD",   1),
        new Holding(ResourceIcons.Kind.BRICK,  "BRICK",  2),
        new Holding(ResourceIcons.Kind.WHEAT,  "WHEAT",  3),
        new Holding(ResourceIcons.Kind.ORE,    "ORE",    1),
        new Holding(ResourceIcons.Kind.BANANA, "BANANA", 2)
    );
    private static final int REQUIRED = 4;

    @FXML private StackPane root;
    @FXML private HBox columns;
    @FXML private Label progressLabel;
    @FXML private Button confirmBtn;

    private final List<Stepper> steppers = new ArrayList<>();

    @FXML
    public void initialize() {
        for (Holding h : HOLDINGS) {
            VBox col = new VBox(4);
            col.setAlignment(Pos.CENTER);
            col.setStyle("-fx-padding: 12; -fx-background-color: rgba(255,255,255,0.6);"
                + " -fx-border-color: -parchment-line; -fx-border-radius: 10;"
                + " -fx-background-radius: 10;");
            HBox.setHgrow(col, Priority.ALWAYS);

            StackPane icon = new StackPane(ResourceIcons.of(h.kind()));
            icon.setMinSize(30, 30); icon.setMaxSize(30, 30);
            Label name = new Label(h.label());
            name.getStyleClass().add("eyebrow");
            Label hold = new Label("Hold: " + h.hold());
            hold.setStyle("-fx-font-size: 11px; -fx-text-fill: -ink-mute;");
            Stepper stepper = new Stepper(0, 0, h.hold());
            stepper.valueProperty().addListener((obs, oldV, newV) -> refreshProgress());
            steppers.add(stepper);

            col.getChildren().addAll(icon, name, hold, stepper);
            columns.getChildren().add(col);
        }
        refreshProgress();
    }

    private void refreshProgress() {
        int total = 0;
        for (Stepper s : steppers) total += s.valueProperty().get();
        progressLabel.setText("Selecting " + total + " / " + REQUIRED);
        confirmBtn.setDisable(total != REQUIRED);
    }

    @FXML
    private void onConfirm() {
        System.out.println("[Discard] confirm");
        close();
    }

    @FXML
    private void onClose() { close(); }

    private void close() {
        Navigator.closeOverlay(root);
    }
}
