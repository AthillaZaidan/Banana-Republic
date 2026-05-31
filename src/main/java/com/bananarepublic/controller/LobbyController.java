package com.bananarepublic.controller;

import com.bananarepublic.engine.BoardMode;
import com.bananarepublic.engine.GameConfig;
import com.bananarepublic.engine.GameEngine;
import com.bananarepublic.engine.PlayerConfig;
import com.bananarepublic.model.player.PlayerColor;
import com.bananarepublic.ui.AudioEngine;
import com.bananarepublic.ui.GameSession;
import com.bananarepublic.ui.LivingBackground;
import com.bananarepublic.ui.Navigator;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LobbyController {
    private static final List<String> COLORS = List.of("red", "blue", "gold", "white");
    private static final String[] DEFAULT_NAMES = {"Stewart", "Gro", "Kebin", "Tara"};

    @FXML private Pane livingLayer;
    @FXML private VBox playerRows;
    @FXML private ComboBox<Integer> playerCountBox;
    @FXML private Label mapPluginLabel;
    @FXML private Label botPluginLabel;
    @FXML private Button startBtn;

    private final List<PlayerRow> rows = new ArrayList<>();
    private final Map<String, PlayerRow> selectedColorOwner = new LinkedHashMap<>();

    @FXML
    public void initialize() {
        LivingBackground.attach(livingLayer, LivingBackground.Variant.PARCHMENT);
        playerCountBox.getItems().setAll(3, 4);
        playerCountBox.valueProperty().addListener((obs, oldV, newV) -> rebuildPlayerRows(newV));
        playerCountBox.setValue(4);
    }

    private void rebuildPlayerRows(Integer count) {
        if (count == null) count = 4;
        playerRows.getChildren().clear();
        rows.clear();
        selectedColorOwner.clear();
        for (int i = 0; i < count; i++) {
            PlayerRow row = new PlayerRow(i + 1, DEFAULT_NAMES[i % DEFAULT_NAMES.length],
                COLORS.get(i % COLORS.size()));
            rows.add(row);
            playerRows.getChildren().add(row.node);
            selectedColorOwner.put(row.selectedColor, row);
        }
        refreshSwatchStates();
    }

    private void refreshSwatchStates() {
        for (PlayerRow row : rows) {
            for (Map.Entry<String, Region> entry : row.swatches.entrySet()) {
                String color = entry.getKey();
                Region swatch = entry.getValue();
                swatch.getStyleClass().removeAll("is-selected", "is-taken");
                if (color.equals(row.selectedColor)) {
                    swatch.getStyleClass().add("is-selected");
                } else if (selectedColorOwner.containsKey(color)
                           && selectedColorOwner.get(color) != row) {
                    swatch.getStyleClass().add("is-taken");
                }
            }
        }
    }

    private void selectColor(PlayerRow row, String color) {
        if (selectedColorOwner.containsKey(color) && selectedColorOwner.get(color) != row) {
            AudioEngine.get().playSfx(AudioEngine.Sfx.ERROR);
            return;
        }
        AudioEngine.get().playSfx(AudioEngine.Sfx.CLICK);
        selectedColorOwner.remove(row.selectedColor);
        row.selectedColor = color;
        selectedColorOwner.put(color, row);
        refreshSwatchStates();
    }

    @FXML
    private void onBack() {
        AudioEngine.get().playSfx(AudioEngine.Sfx.CLICK);
        Navigator.goTo("/fxml/main_menu.fxml");
    }

    @FXML
    private void onBrowseMapPlugin() {
        AudioEngine.get().playSfx(AudioEngine.Sfx.CLICK);
        File f = pickJar("Select Map Plugin");
        if (f != null) {
            mapPluginLabel.setText(f.getName());
        }
    }

    @FXML
    private void onBrowseBotPlugin() {
        AudioEngine.get().playSfx(AudioEngine.Sfx.CLICK);
        File f = pickJar("Select Bot Plugin");
        if (f != null) {
            botPluginLabel.setText(f.getName());
        }
    }

    private File pickJar(String title) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Plugin Jar", "*.jar"));
        return chooser.showOpenDialog(Navigator.primary());
    }

    @FXML
    private void onStartGame() {
        AudioEngine.get().playSfx(AudioEngine.Sfx.CLICK);
        java.util.List<PlayerConfig> configs = new java.util.ArrayList<>();
        for (PlayerRow row : rows) {
            String name = row.nameField.getText().isBlank() ? "Player " + row.index : row.nameField.getText();
            configs.add(new PlayerConfig(name, toEngineColor(row.selectedColor)));
            System.out.printf("[Lobby] Player %d: %s (%s)%n",
                row.index, name, row.selectedColor);
        }
        GameEngine engine = new GameEngine();
        engine.startNewGame(new GameConfig(configs, BoardMode.FIXED, true));
        GameSession.setEngine(engine);
        GameSession.markSessionStartNow();
        GameSession.setStartingOrderPending(true);
        Navigator.goTo("/fxml/game.fxml");
    }

    private static PlayerColor toEngineColor(String css) {
        return switch (css) {
            case "red"   -> PlayerColor.RED;
            case "blue"  -> PlayerColor.BLUE;
            case "gold"  -> PlayerColor.YELLOW;
            case "white" -> PlayerColor.GREEN;
            default -> PlayerColor.RED;
        };
    }

    private final class PlayerRow {
        final int index;
        final HBox node;
        final TextField nameField;
        final Map<String, Region> swatches = new LinkedHashMap<>();
        String selectedColor;

        PlayerRow(int index, String name, String color) {
            this.index = index;
            this.selectedColor = color;

            StackPane numberChip = new StackPane();
            numberChip.setMinSize(28, 28); numberChip.setMaxSize(28, 28);
            numberChip.setStyle("-fx-background-color: #fff8e1; -fx-background-radius: 999;"
                + " -fx-border-color: -parchment-line; -fx-border-radius: 999; -fx-border-width: 1;");
            Label numberLabel = new Label(String.valueOf(index));
            numberLabel.setStyle("-fx-text-fill: -gold-deep; -fx-font-weight: 900; -fx-font-family: Georgia;");
            numberChip.getChildren().add(numberLabel);

            nameField = new TextField(name);
            HBox.setHgrow(nameField, javafx.scene.layout.Priority.ALWAYS);

            HBox swatchRow = new HBox(6);
            for (String c : COLORS) {
                Region s = new Region();
                s.getStyleClass().addAll("swatch", "sw-" + c);
                s.setOnMouseClicked(e -> selectColor(this, c));
                swatches.put(c, s);
                swatchRow.getChildren().add(s);
            }

            node = new HBox(14, numberChip, nameField, swatchRow);
            node.setStyle("-fx-padding: 10 12; -fx-background-radius: 10;"
                + " -fx-background-color: rgba(255, 248, 225, 0.55);"
                + " -fx-border-color: -parchment-line; -fx-border-radius: 10; -fx-border-width: 1;");
            node.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        }
    }
}
