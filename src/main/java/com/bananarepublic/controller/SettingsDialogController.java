package com.bananarepublic.controller;

import com.bananarepublic.plugin.PluginLoadException;
import com.bananarepublic.ui.GameSession;
import com.bananarepublic.ui.LivingBackground;
import com.bananarepublic.ui.Navigator;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;

import java.io.File;

public class SettingsDialogController {
    @FXML private StackPane root;
    @FXML private CheckBox animatedBgToggle;

    @FXML
    public void initialize() {
        animatedBgToggle.setSelected(LivingBackground.isAnimationsEnabled());
    }

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
        if (f == null) return;

        if (!GameSession.hasEngine()) {
            showAlert("Error", "Tidak ada permainan aktif untuk memuat plugin.");
            return;
        }

        try {
            GameSession.engine().loadPluginCards(f);
            showAlert("Sukses", "Plugin berhasil dimuat: " + f.getName());
        } catch (PluginLoadException ex) {
            showAlert("Gagal Memuat Plugin", ex.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void onApply() {
        LivingBackground.setAnimationsEnabled(animatedBgToggle.isSelected());
        System.out.println("[Settings] apply (animations="
            + animatedBgToggle.isSelected() + ")");
        close();
    }

    @FXML
    private void onClose() { close(); }

    private void close() {
        Navigator.closeOverlay(root);
    }
}
