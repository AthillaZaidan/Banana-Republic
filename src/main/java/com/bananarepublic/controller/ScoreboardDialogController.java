package com.bananarepublic.controller;

import com.bananarepublic.engine.GameState;
import com.bananarepublic.model.building.BuildingType;
import com.bananarepublic.model.player.Player;
import com.bananarepublic.model.player.PlayerColor;
import com.bananarepublic.model.player.SpecialCardType;
import com.bananarepublic.service.victory.VictoryService;
import com.bananarepublic.ui.AudioEngine;
import com.bananarepublic.ui.GameSession;
import com.bananarepublic.ui.Navigator;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ScoreboardDialogController {
    @FXML private StackPane root;
    @FXML private Label summaryBand;
    @FXML private VBox rowsBox;

    private final VictoryService victoryService = new VictoryService();

    @FXML
    public void initialize() {
        if (!GameSession.hasEngine()) {
            summaryBand.setText("Tidak ada permainan aktif.");
            rowsBox.getChildren().setAll(createEmptyState());
            return;
        }

        GameState state = GameSession.engine().getState();
        Player active = state.getCurrentPlayer();
        summaryBand.setText(active.getName() + " sedang aktif. Peringkat diurutkan berdasarkan total victory points.");

        List<Player> ranking = new ArrayList<>(state.getPlayers());
        ranking.sort(Comparator
                .comparingInt((Player player) -> victoryService.calculateVictoryPoints(player))
                .reversed()
                .thenComparing(Player::getName));

        List<VBox> rows = new ArrayList<>(ranking.size());
        for (int i = 0; i < ranking.size(); i++) {
            Player player = ranking.get(i);
            rows.add(createPlayerRow(i + 1, player, player.equals(active)));
        }
        rowsBox.getChildren().setAll(rows);
    }

    @FXML
    private void onClose() {
        AudioEngine.get().playSfx(AudioEngine.Sfx.CLICK);
        Navigator.closeOverlay(root);
    }

    private VBox createPlayerRow(int rank, Player player, boolean active) {
        VBox row = new VBox(8);
        row.getStyleClass().addAll("card-dark", "scoreboard-row");
        if (active) {
            row.getStyleClass().add("scoreboard-row-active");
        }

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Label rankLabel = new Label("#" + rank);
        rankLabel.getStyleClass().add("scoreboard-rank");

        StackPane initial = new StackPane(new Label(String.valueOf(player.getName().charAt(0))));
        initial.getStyleClass().addAll("initial-chip", "pc-" + cssColor(player.getColor()));
        initial.setMinSize(34, 34);
        initial.setPrefSize(34, 34);
        initial.setMaxSize(34, 34);

        VBox identity = new VBox(4);
        Label nameLabel = new Label(player.getName());
        nameLabel.getStyleClass().add("scoreboard-name");

        HBox badgeRow = new HBox(6);
        if (active) {
            badgeRow.getChildren().add(createTag("CURRENT TURN", true));
        }
        if (player.hasSpecialCard(SpecialCardType.LONGEST_ROAD)) {
            badgeRow.getChildren().add(createTag("LONGEST ROAD", false));
        }
        if (player.hasSpecialCard(SpecialCardType.LARGEST_ARMY)) {
            badgeRow.getChildren().add(createTag("LARGEST ARMY", false));
        }
        identity.getChildren().add(nameLabel);
        if (!badgeRow.getChildren().isEmpty()) {
            identity.getChildren().add(badgeRow);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox vpBox = new VBox(2);
        vpBox.setAlignment(Pos.CENTER_RIGHT);
        Label vpHint = new Label("TOTAL VP");
        vpHint.getStyleClass().add("scoreboard-kicker");
        Label vpValue = new Label(String.valueOf(victoryService.calculateVictoryPoints(player)));
        vpValue.getStyleClass().add("scoreboard-vp");
        vpBox.getChildren().addAll(vpHint, vpValue);

        header.getChildren().addAll(rankLabel, initial, identity, spacer, vpBox);

        FlowPane stats = new FlowPane();
        stats.setHgap(8);
        stats.setVgap(8);
        stats.setPrefWrapLength(380);
        stats.getStyleClass().add("scoreboard-stats");
        stats.getChildren().addAll(
                createStat("Pipes", player.getOwnedPipes().size()),
                createStat("Posts", ownedPosts(player)),
                createStat("Labs", ownedLabs(player)),
                createStat("Knights", player.getPlayedKnightCount()),
                createStat("Resources", player.getTotalResourceCards()),
                createStat("Cards", player.getHandCardCount())
        );

        row.getChildren().addAll(header, stats);
        return row;
    }

    private Label createTag(String text, boolean currentTurn) {
        Label label = new Label(text);
        label.getStyleClass().add("scoreboard-tag");
        if (currentTurn) {
            label.getStyleClass().add("scoreboard-tag-active");
        }
        return label;
    }

    private VBox createStat(String label, int value) {
        VBox stat = new VBox(2);
        stat.getStyleClass().add("scoreboard-stat");
        Label valueLabel = new Label(String.valueOf(value));
        valueLabel.getStyleClass().add("scoreboard-stat__value");
        Label textLabel = new Label(label);
        textLabel.getStyleClass().add("scoreboard-stat__label");
        stat.getChildren().addAll(valueLabel, textLabel);
        return stat;
    }

    private Label createEmptyState() {
        Label label = new Label("Scoreboard tidak tersedia karena tidak ada game aktif.");
        label.getStyleClass().add("scoreboard-empty");
        label.setWrapText(true);
        return label;
    }

    private static int ownedPosts(Player player) {
        return (int) player.getOwnedBuildings().stream()
                .filter(building -> building.getType() == BuildingType.MONITORING_POST)
                .count();
    }

    private static int ownedLabs(Player player) {
        return (int) player.getOwnedBuildings().stream()
                .filter(building -> building.getType() == BuildingType.LABORATORY)
                .count();
    }

    private static String cssColor(PlayerColor color) {
        return switch (color) {
            case RED -> "red";
            case BLUE -> "blue";
            case YELLOW -> "gold";
            case GREEN -> "white";
        };
    }
}
