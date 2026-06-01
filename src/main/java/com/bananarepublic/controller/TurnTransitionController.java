package com.bananarepublic.controller;

import com.bananarepublic.model.building.BuildingType;
import com.bananarepublic.model.player.Player;
import com.bananarepublic.model.player.PlayerColor;
import com.bananarepublic.model.player.SpecialCardType;
import com.bananarepublic.service.victory.VictoryService;
import com.bananarepublic.ui.AudioEngine;
import com.bananarepublic.ui.GameIcons;
import com.bananarepublic.ui.GameSession;
import com.bananarepublic.ui.LivingBackground;
import com.bananarepublic.ui.Navigator;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

public class TurnTransitionController {
    @FXML private Pane livingLayer;
    @FXML private StackPane playerSeal;
    @FXML private StackPane personIcon;
    @FXML private Label nextPlayerLabel;
    @FXML private Label playerMetaLabel;
    @FXML private Label phaseHintLabel;
    @FXML private Label publicVpLabel;
    @FXML private Label pipesLabel;
    @FXML private Label postsLabel;
    @FXML private Label labsLabel;
    @FXML private Label cardsLabel;
    @FXML private FlowPane badgeRow;

    private final VictoryService victoryService = new VictoryService();

    @FXML
    public void initialize() {
        LivingBackground.attach(livingLayer, LivingBackground.Variant.SLATE);
        personIcon.getChildren().setAll(GameIcons.person());

        if (!GameSession.hasEngine()) {
            return;
        }

        Player player = GameSession.engine().getState().getCurrentPlayer();
        playerSeal.getStyleClass().removeAll("pc-red", "pc-blue", "pc-gold", "pc-white");
        playerSeal.getStyleClass().add("pc-" + cssColor(player.getColor()));

        nextPlayerLabel.setText(player.getName());
        playerMetaLabel.setText(colorName(player.getColor()) + " Team");
        phaseHintLabel.setText("Awali giliran dengan melempar dadu, lalu lanjutkan fase Trade/Build.");

        publicVpLabel.setText(String.valueOf(victoryService.calculateVictoryPoints(player)));
        pipesLabel.setText(player.getOwnedPipes().size() + "/15");
        postsLabel.setText(String.valueOf(ownedPosts(player)));
        labsLabel.setText(String.valueOf(ownedLabs(player)));
        cardsLabel.setText(String.valueOf(player.getHandCardCount()));

        badgeRow.getChildren().clear();
        if (player.hasSpecialCard(SpecialCardType.LONGEST_ROAD)) {
            badgeRow.getChildren().add(buildBadge("LONGEST ROAD"));
        }
        if (player.hasSpecialCard(SpecialCardType.LARGEST_ARMY)) {
            badgeRow.getChildren().add(buildBadge("LARGEST ARMY"));
        }
        if (badgeRow.getChildren().isEmpty()) {
            Label note = new Label("Tidak ada kartu publik aktif pada pemain ini.");
            note.getStyleClass().add("turn-transition-note");
            badgeRow.getChildren().add(note);
        }
    }

    @FXML
    private void onReady() {
        AudioEngine.get().playSfx(AudioEngine.Sfx.CLICK);
        Navigator.goTo("/fxml/game.fxml");
    }

    private Label buildBadge(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("scoreboard-tag");
        return label;
    }

    private int ownedPosts(Player player) {
        return (int) player.getOwnedBuildings().stream()
                .filter(building -> building.getType() == BuildingType.MONITORING_POST)
                .count();
    }

    private int ownedLabs(Player player) {
        return (int) player.getOwnedBuildings().stream()
                .filter(building -> building.getType() == BuildingType.LABORATORY)
                .count();
    }

    private String colorName(PlayerColor color) {
        return switch (color) {
            case RED -> "Red";
            case BLUE -> "Blue";
            case YELLOW -> "Gold";
            case GREEN -> "White";
        };
    }

    private String cssColor(PlayerColor color) {
        return switch (color) {
            case RED -> "red";
            case BLUE -> "blue";
            case YELLOW -> "gold";
            case GREEN -> "white";
        };
    }
}
