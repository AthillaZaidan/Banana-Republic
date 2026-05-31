package com.bananarepublic.controller;

import com.bananarepublic.exception.SaveLoadException;
import com.bananarepublic.plugin.PluginLoadException;
import com.bananarepublic.ui.AudioEngine;
import com.bananarepublic.ui.GameSession;
import com.bananarepublic.ui.LivingBackground;
import com.bananarepublic.ui.Navigator;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;

import java.io.File;

public class SettingsDialogController {
    @FXML private StackPane root;
    @FXML private CheckBox animatedBgToggle;
    @FXML private Slider bgmSlider;
    @FXML private Slider sfxSlider;
    @FXML private Label bgmValueLabel;
    @FXML private Label sfxValueLabel;

    @FXML
    public void initialize() {
        animatedBgToggle.setSelected(LivingBackground.isAnimationsEnabled());

        AudioEngine audio = AudioEngine.get();
        bgmSlider.setValue(audio.getBgmVolume() * 100);
        sfxSlider.setValue(audio.getSfxVolume() * 100);

        updateLabels();

        bgmSlider.valueProperty().addListener((obs, o, n) -> {
            audio.setBgmVolume(n.doubleValue() / 100.0);
            updateLabels();
        });
        sfxSlider.valueProperty().addListener((obs, o, n) -> {
            audio.setSfxVolume(n.doubleValue() / 100.0);
            updateLabels();
        });
    }

    private void updateLabels() {
        bgmValueLabel.setText((int) bgmSlider.getValue() + "%");
        sfxValueLabel.setText((int) sfxSlider.getValue() + "%");
    }

    @FXML
    private void onSaveState() {
        AudioEngine.get().playSfx(AudioEngine.Sfx.CLICK);
        if (!GameSession.hasEngine()) {
            showAlert("Error", "Tidak ada permainan aktif untuk disimpan.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Game State");
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Game Save", "*.json", "*.ser"));
        File f = chooser.showSaveDialog(Navigator.primary());
        if (f != null) {
            File target = normalizeSaveTarget(f);
            try {
                GameSession.engine().saveGame(target);
                showAlert("Sukses", "Save berhasil ditulis ke: " + target.getName());
            } catch (SaveLoadException ex) {
                showAlert("Gagal Menyimpan Save", ex.getMessage());
            }
        }
    }

    @FXML
    private void onBrowsePlugin() {
        AudioEngine.get().playSfx(AudioEngine.Sfx.CLICK);
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
        AudioEngine.get().playSfx(AudioEngine.Sfx.CLICK);
        LivingBackground.setAnimationsEnabled(animatedBgToggle.isSelected());
        System.out.println("[Settings] apply (animations="
            + animatedBgToggle.isSelected() + ")");
        close();
    }

    @FXML
    private void onClose() { AudioEngine.get().playSfx(AudioEngine.Sfx.CLICK); close(); }

    private void close() {
        Navigator.closeOverlay(root);
    }

    private File normalizeSaveTarget(File chosen) {
        String lowerName = chosen.getName().toLowerCase();
        if (lowerName.endsWith(".json") || lowerName.endsWith(".ser")) {
            return chosen;
        }
        return new File(chosen.getParentFile(), chosen.getName() + ".json");
    }
}
