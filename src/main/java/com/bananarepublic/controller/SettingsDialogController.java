package com.bananarepublic.controller;

import com.bananarepublic.ui.Navigator;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class SettingsDialogController {
    @FXML private StackPane root;

    @FXML
    private void onSaveState() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Game State");
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Game Save", "*.json", "*.ser"));
        File f = chooser.showSaveDialog(Navigator.primary());
        if (f != null) {
            System.out.println("[Settings] save -> " + f.getAbsolutePath());
        }
    }

    @FXML
    private void onBrowsePlugin() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Load Card Plugin");
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Plugin Jar", "*.jar"));
        File f = chooser.showOpenDialog(Navigator.primary());
        if (f != null) {
            System.out.println("[Settings] plugin -> " + f.getAbsolutePath());
        }
    }

    @FXML
    private void onApply() {
        System.out.println("[Settings] apply");
        close();
    }

    @FXML
    private void onClose() { close(); }

    private void close() {
        Node source = root != null ? root : null;
        if (source == null || source.getScene() == null) return;
        Stage st = (Stage) source.getScene().getWindow();
        st.close();
    }
}
