package com.bananarepublic.controller;

import com.bananarepublic.ui.AudioEngine;
import com.bananarepublic.ui.GameSession;
import com.bananarepublic.ui.Navigator;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;

public class StealDialogController {
    private record Target(String playerId, String name, String color, int cards) {}

    @FXML private StackPane root;
    @FXML private HBox targetRow;

    private VBox selected;
    private String selectedPlayerId;

    @FXML
    public void initialize() {
        if (!GameSession.hasEngine()) {
            close();
            return;
        }

        List<Target> targets = GameSession.engine().getValidStealTargetsAfterSeven().stream()
                .map(player -> new Target(
                        player.getId(),
                        player.getName(),
                        cssColor(player.getColor()),
                        player.getTotalResourceCards()
                ))
                .toList();

        if (targets.isEmpty()) {
            try {
                GameSession.engine().finishNimonAfterSevenWithoutSteal();
            } catch (RuntimeException ignored) {
                // Ignore when overlay opened from a non-seven flow.
            }
            close();
            return;
        }

        boolean first = true;
        for (Target t : targets) {
            VBox tile = buildTile(t, first);
            if (first) {
                selected = tile;
                selectedPlayerId = t.playerId();
            }
            HBox.setHgrow(tile, Priority.ALWAYS);
            targetRow.getChildren().add(tile);
            first = false;
        }
    }

    private VBox buildTile(Target t, boolean isSelected) {
        VBox tile = new VBox(6);
        tile.setAlignment(Pos.CENTER);
        applyTileStyle(tile, isSelected);

        StackPane chip = new StackPane(new Label(String.valueOf(t.name().charAt(0))));
        chip.getStyleClass().addAll("initial-chip", "pc-" + t.color());
        chip.setMinSize(44, 44); chip.setMaxSize(44, 44);
        ((Label) chip.getChildren().get(0)).setStyle("-fx-font-size: 20px;");

        Label name = new Label(t.name());
        name.setStyle("-fx-font-weight: 800; -fx-font-size: 14px;");

        Label cards = new Label(t.cards() + " Resource Cards");
        cards.setStyle("-fx-font-size: 11px; -fx-text-fill: -ink-mute;");

        Label random = new Label("?");
        random.setStyle("-fx-background-color: linear-gradient(to bottom, #fff8e1, #ead7a4);"
            + " -fx-border-color: -parchment-line; -fx-border-style: dashed;"
            + " -fx-border-radius: 8; -fx-background-radius: 8;"
            + " -fx-padding: 10 30; -fx-text-fill: -gold-deep;"
            + " -fx-font-weight: 900; -fx-font-size: 22px;");

        Label tag = new Label("RANDOM ITEM");
        tag.getStyleClass().add("eyebrow");
        tag.setStyle("-fx-font-size: 9px;");

        tile.getChildren().addAll(chip, name, cards, random, tag);
        tile.setOnMouseClicked(e -> select(tile, t.playerId()));
        return tile;
    }

    private void applyTileStyle(VBox tile, boolean selected) {
        tile.setStyle("-fx-padding: 16; -fx-background-radius: 12;"
            + " -fx-background-color: rgba(255,255,255,0.6);"
            + (selected
                ? "-fx-border-color: -gold-1; -fx-border-width: 2.5; -fx-border-radius: 12;"
                  + " -fx-effect: dropshadow(gaussian, rgba(245,183,56,0.25), 8, 0.4, 0, 0);"
                : "-fx-border-color: -parchment-line; -fx-border-width: 1.5; -fx-border-radius: 12;"));
    }

    private void select(VBox tile, String playerId) {
        if (selected != null) applyTileStyle(selected, false);
        selected = tile;
        selectedPlayerId = playerId;
        applyTileStyle(tile, true);
    }

    @FXML
    private void onConfirm() {
        try {
            if (selectedPlayerId != null) {
                AudioEngine.get().playSfx(AudioEngine.Sfx.DAGGER);
                GameSession.engine().stealAfterSeven(selectedPlayerId);
            } else {
                GameSession.engine().finishNimonAfterSevenWithoutSteal();
            }
            GameController controller = GameSession.getGameController();
            if (controller != null) {
                controller.log("[Nimon] Steal action resolved.");
                controller.refresh();
            }
        } catch (RuntimeException ex) {
            return;
        }
        close();
    }

    @FXML
    private void onClose() { close(); }

    private void close() {
        if (GameSession.hasEngine()) {
            try {
                GameSession.engine().finishNimonAfterSevenWithoutSteal();
                GameController controller = GameSession.getGameController();
                if (controller != null) {
                    controller.log("[Nimon] Steal skipped.");
                    controller.refresh();
                }
            } catch (RuntimeException ignored) {
                // Overlay can also close after a resolved steal or outside the seven flow.
            }
        }
        Navigator.closeOverlay(root);
    }

    private static String cssColor(com.bananarepublic.model.player.PlayerColor color) {
        return switch (color) {
            case RED -> "red";
            case BLUE -> "blue";
            case YELLOW -> "gold";
            case GREEN -> "white";
        };
    }
}
