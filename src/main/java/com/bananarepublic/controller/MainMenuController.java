package com.bananarepublic.controller;

import com.bananarepublic.engine.GameEngine;
import com.bananarepublic.exception.SaveLoadException;
import com.bananarepublic.ui.GameSession;
import com.bananarepublic.ui.LivingBackground;
import com.bananarepublic.ui.Navigator;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;

import java.io.File;

public class MainMenuController {
    @FXML private Pane livingLayer;

    @FXML
    public void initialize() {
        LivingBackground.attach(livingLayer, LivingBackground.Variant.PARCHMENT);
    }

    @FXML
    private void onNewGame() {
        System.out.println("[MainMenu] NEW GAME");
        Navigator.goTo("/fxml/lobby.fxml");
    }

    @FXML
    private void onLoadGame() {
        System.out.println("[MainMenu] LOAD GAME");
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Load Game");
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Game Save", "*.json", "*.ser"));
        File chosen = chooser.showOpenDialog(Navigator.primary());
        if (chosen != null) {
            try {
                GameEngine engine = new GameEngine();
                engine.loadGame(chosen);
                GameSession.setEngine(engine);
                Navigator.goTo("/fxml/game.fxml");
            } catch (SaveLoadException ex) {
                showAlert("Gagal Memuat Save", ex.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    @FXML
    private void onExit() {
        System.out.println("[MainMenu] EXIT");
        Navigator.primary().close();
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
