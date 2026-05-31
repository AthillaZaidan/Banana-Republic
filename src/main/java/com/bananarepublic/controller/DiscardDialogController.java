package com.bananarepublic.controller;

import com.bananarepublic.engine.GameEngine;
import com.bananarepublic.model.player.Player;
import com.bananarepublic.model.resource.ResourceInventory;
import com.bananarepublic.model.resource.ResourceType;
import com.bananarepublic.ui.Navigator;
import com.bananarepublic.ui.ResourceIcons;
import com.bananarepublic.ui.Stepper;
import com.bananarepublic.ui.GameSession;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DiscardDialogController {
    private record Holding(ResourceType type, ResourceIcons.Kind kind, String label, int hold) {}

    @FXML private StackPane root;
    @FXML private HBox columns;
    @FXML private Label ruleText;
    @FXML private Label progressLabel;
    @FXML private Button confirmBtn;

    private int required;
    private Player discardPlayer;
    private final List<Stepper> steppers = new ArrayList<>();
    private final Map<ResourceType, Stepper> steppersByType = new EnumMap<>(ResourceType.class);

    @FXML
    public void initialize() {
        if (!GameSession.hasEngine()) {
            close();
            return;
        }
        if (!loadDiscardPlayer()) {
            close();
            return;
        }
        rebuildColumns();
        refreshProgress();
    }

    private void refreshProgress() {
        int total = 0;
        for (Stepper s : steppers) total += s.valueProperty().get();
        progressLabel.setText("Selecting " + total + " / " + required);
        confirmBtn.setDisable(total != required);
    }

    @FXML
    private void onConfirm() {
        ResourceInventory discarded = new ResourceInventory();
        for (Map.Entry<ResourceType, Stepper> entry : steppersByType.entrySet()) {
            int amount = entry.getValue().valueProperty().get();
            if (amount > 0) {
                discarded.add(entry.getKey(), amount);
            }
        }
        try {
            GameSession.engine().discardForSeven(discardPlayer.getId(), discarded);
            GameController controller = GameSession.getGameController();
            if (controller != null) {
                controller.log("[Nimon] " + discardPlayer.getName() + " discarded " + required + " resources.");
                controller.refresh();
            }
        } catch (RuntimeException ex) {
            progressLabel.setText(ex.getMessage());
            return;
        }

        if (loadDiscardPlayer()) {
            rebuildColumns();
            refreshProgress();
            return;
        }

        close();
        GameController controller = GameSession.getGameController();
        if (controller != null) {
            Platform.runLater(controller::continueSpecialTurnFlow);
        }
    }

    @FXML
    private void onClose() {
        progressLabel.setText("Complete the required discard to continue.");
    }

    private void close() {
        Navigator.closeOverlay(root);
    }

    private boolean loadDiscardPlayer() {
        GameEngine engine = GameSession.engine();
        Set<String> pendingPlayerIds = engine.getState().getTurnState().getPendingDiscardPlayerIds();
        discardPlayer = engine.getState().getPlayers().stream()
                .filter(player -> pendingPlayerIds.contains(player.getId()))
                .findFirst()
                .orElse(null);
        if (discardPlayer == null) {
            required = 0;
            return false;
        }

        required = discardPlayer.getTotalResourceCards() / 2;
        ruleText.setText("A 7 was rolled. " + discardPlayer.getName() + " holds "
                + discardPlayer.getTotalResourceCards() + " cards and must discard floor("
                + discardPlayer.getTotalResourceCards() + "/2) = " + required + ".");
        return true;
    }

    private void rebuildColumns() {
        columns.getChildren().clear();
        steppers.clear();
        steppersByType.clear();

        List<Holding> holdings = List.of(
                new Holding(ResourceType.WOOD, ResourceIcons.Kind.WOOD, "WOOD", discardPlayer.getResourceAmount(ResourceType.WOOD)),
                new Holding(ResourceType.BRICK, ResourceIcons.Kind.BRICK, "BRICK", discardPlayer.getResourceAmount(ResourceType.BRICK)),
                new Holding(ResourceType.WHEAT, ResourceIcons.Kind.WHEAT, "WHEAT", discardPlayer.getResourceAmount(ResourceType.WHEAT)),
                new Holding(ResourceType.ORE, ResourceIcons.Kind.ORE, "ORE", discardPlayer.getResourceAmount(ResourceType.ORE)),
                new Holding(ResourceType.BANANA, ResourceIcons.Kind.BANANA, "BANANA", discardPlayer.getResourceAmount(ResourceType.BANANA))
        );

        for (Holding h : holdings) {
            VBox col = new VBox(4);
            col.setAlignment(Pos.CENTER);
            col.setStyle("-fx-padding: 12; -fx-background-color: rgba(255,255,255,0.6);"
                    + " -fx-border-color: -parchment-line; -fx-border-radius: 10;"
                    + " -fx-background-radius: 10;");
            HBox.setHgrow(col, Priority.ALWAYS);

            StackPane icon = new StackPane(ResourceIcons.of(h.kind()));
            icon.setMinSize(30, 30);
            icon.setMaxSize(30, 30);
            Label name = new Label(h.label());
            name.getStyleClass().add("eyebrow");
            Label hold = new Label("Hold: " + h.hold());
            hold.setStyle("-fx-font-size: 11px; -fx-text-fill: -ink-mute;");
            Stepper stepper = new Stepper(0, 0, h.hold());
            stepper.valueProperty().addListener((obs, oldV, newV) -> refreshProgress());
            steppers.add(stepper);
            steppersByType.put(h.type(), stepper);

            col.getChildren().addAll(icon, name, hold, stepper);
            columns.getChildren().add(col);
        }
    }
}
