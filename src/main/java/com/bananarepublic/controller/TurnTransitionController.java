package com.bananarepublic.controller;

import com.bananarepublic.ui.LivingBackground;
import com.bananarepublic.ui.Navigator;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;

public class TurnTransitionController {
    @FXML private Pane livingLayer;
    @FXML private Label nextPlayerLabel;

    @FXML
    public void initialize() {
        LivingBackground.attach(livingLayer, LivingBackground.Variant.SLATE);
    }

    @FXML
    private void onReady() {
        System.out.println("[TurnTransition] ready");
        Navigator.goTo("/fxml/game.fxml");
    }
}
