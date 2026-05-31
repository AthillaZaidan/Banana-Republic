package com.bananarepublic.controller;

import com.bananarepublic.service.dice.DiceMode;
import com.bananarepublic.service.dice.DiceRoll;
import com.bananarepublic.ui.DiceDialogRequest;
import com.bananarepublic.ui.DiceDialogResult;
import com.bananarepublic.ui.DicePips;
import com.bananarepublic.ui.GameSession;
import com.bananarepublic.ui.AudioEngine;
import com.bananarepublic.ui.Navigator;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class DiceRollDialogController {
    @FXML private StackPane root;
    @FXML private Label titleLabel;
    @FXML private Label headerLabel;
    @FXML private Label tabRandom;
    @FXML private Label tabManual;
    @FXML private VBox manualSection;
    @FXML private HBox firstDieRow;
    @FXML private HBox secondDieRow;
    @FXML private Pane firstDiePreview;
    @FXML private Pane secondDiePreview;
    @FXML private Label helperLabel;
    @FXML private Button confirmBtn;

    private DiceDialogRequest request;
    private Integer firstDieValue = 1;
    private Integer secondDieValue = 1;
    private boolean manualMode;

    @FXML
    public void initialize() {
        request = GameSession.getDiceDialogRequest();
        if (request == null) {
            request = new DiceDialogRequest("Roll Dice", "Choose how to roll.", "ROLL", true);
        }

        titleLabel.setText(request.title());
        headerLabel.setText(request.header());
        confirmBtn.setText(request.confirmLabel());

        buildDieRow(firstDieRow, true);
        buildDieRow(secondDieRow, false);

        if (!request.manualEnabled()) {
            manualMode = false;
            tabManual.setDisable(true);
            tabManual.setOpacity(0.45);
        }
        applyModeStyles();
        refreshManualUi();
    }

    @FXML
    private void onSelectRandom() {
        AudioEngine.get().playSfx(AudioEngine.Sfx.CLICK);
        manualMode = false;
        applyModeStyles();
    }

    @FXML
    private void onSelectManual() {
        if (!request.manualEnabled()) {
            return;
        }
        AudioEngine.get().playSfx(AudioEngine.Sfx.CLICK);
        manualMode = true;
        applyModeStyles();
    }

    @FXML
    private void onConfirm() {
        AudioEngine.get().playSfx(AudioEngine.Sfx.DICE, 700);
        DiceDialogResult result = manualMode
                ? new DiceDialogResult(DiceMode.MANUAL, DiceRoll.of(firstDieValue, secondDieValue))
                : new DiceDialogResult(DiceMode.RANDOM, null);
        closeWithResult(result);
    }

    @FXML
    private void onClose() {
        AudioEngine.get().playSfx(AudioEngine.Sfx.CLICK);
        closeWithResult(null);
    }

    private void buildDieRow(HBox row, boolean firstDie) {
        row.getChildren().clear();
        row.setSpacing(8);
        row.setAlignment(Pos.CENTER_LEFT);
        for (int value = 1; value <= 6; value++) {
            Button button = new Button();
            button.setMinSize(64, 64);
            button.setPrefSize(64, 64);
            button.setMaxSize(64, 64);
            button.setStyle(styleForDieButton(value == 1));
            button.setGraphic(DicePips.createGraphic(value, 50));
            int chosenValue = value;
            button.setOnAction(event -> {
                AudioEngine.get().playSfx(AudioEngine.Sfx.CLICK);
                if (firstDie) {
                    firstDieValue = chosenValue;
                } else {
                    secondDieValue = chosenValue;
                }
                refreshManualUi();
            });
            row.getChildren().add(button);
        }
    }

    private void refreshManualUi() {
        highlightRow(firstDieRow, firstDieValue);
        highlightRow(secondDieRow, secondDieValue);
        renderPreview(firstDiePreview, firstDieValue);
        renderPreview(secondDiePreview, secondDieValue);
        helperLabel.setText(manualMode
                ? "Manual result: " + firstDieValue + " + " + secondDieValue + " = " + (firstDieValue + secondDieValue)
                : "Random mode will roll both dice automatically.");
    }

    private void highlightRow(HBox row, int selectedValue) {
        for (int index = 0; index < row.getChildren().size(); index++) {
            row.getChildren().get(index).setStyle(styleForDieButton(index + 1 == selectedValue));
        }
    }

    private void applyModeStyles() {
        tabRandom.getStyleClass().setAll("tab");
        tabManual.getStyleClass().setAll("tab");
        if (manualMode) {
            tabManual.getStyleClass().add("is-active");
        } else {
            tabRandom.getStyleClass().add("is-active");
        }
        manualSection.setManaged(manualMode);
        manualSection.setVisible(manualMode);
        refreshManualUi();
    }

    private void closeWithResult(DiceDialogResult result) {
        Navigator.closeOverlay(root);
        GameSession.setDiceDialogRequest(null);
        if (GameSession.getGameController() != null) {
            Platform.runLater(() -> GameSession.getGameController().onDiceDialogResolved(result));
        }
    }

    private String styleForDieButton(boolean selected) {
        return "-fx-background-radius: 14; -fx-border-radius: 12; -fx-padding: 0;"
                + (selected
                ? "-fx-background-color: linear-gradient(to bottom, #ffffff, #f5f5f5);"
                + " -fx-border-color: #d8b55e; -fx-border-width: 2.2;"
                + " -fx-effect: dropshadow(gaussian, rgba(245,183,56,0.18), 10, 0.2, 0, 2);"
                : "-fx-background-color: linear-gradient(to bottom, #ffffff, #efefef);"
                + " -fx-border-color: #d5d0c2; -fx-border-width: 1.2;");
    }

    private void renderPreview(Pane target, int value) {
        DicePips.render(target, value, 112);
    }
}
