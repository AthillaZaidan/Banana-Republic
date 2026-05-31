package com.bananarepublic.controller;

import com.bananarepublic.ui.GameIcons;
import com.bananarepublic.ui.GameSession;
import com.bananarepublic.ui.LivingBackground;
import com.bananarepublic.ui.AudioEngine;
import com.bananarepublic.ui.Navigator;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;

public class TurnTransitionController {
    @FXML private Pane livingLayer;
    @FXML private Pane personIcon;
    @FXML private Label nextPlayerLabel;

    @FXML
    public void initialize() {
        LivingBackground.attach(livingLayer, LivingBackground.Variant.SLATE);
        personIcon.getChildren().setAll(GameIcons.person());
        if (GameSession.hasEngine()) {
            var player = GameSession.engine().getState().getCurrentPlayer();
            nextPlayerLabel.setText(player.getName() + " (" + colorName(player.getColor()) + ")");
        }
    }

    @FXML
    private void onReady() {
        AudioEngine.get().playSfx(AudioEngine.Sfx.CLICK);
        Navigator.goTo("/fxml/game.fxml");
    }

    private String colorName(com.bananarepublic.model.player.PlayerColor color) {
        return switch (color) {
            case RED -> "Red";
            case BLUE -> "Blue";
            case YELLOW -> "Gold";
            case GREEN -> "White";
        };
    }
}
