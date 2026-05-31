package com.bananarepublic.controller;

import com.bananarepublic.model.building.BuildingType;
import com.bananarepublic.model.player.Player;
import com.bananarepublic.service.victory.VictoryService;
import com.bananarepublic.ui.GameSession;
import com.bananarepublic.ui.LivingBackground;
import com.bananarepublic.ui.Navigator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.Pane;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;

public class GameResultController {
    @FXML private Pane livingLayer;
    @FXML private Label winnerName;
    @FXML private Label playTime;
    @FXML private Label summaryLabel;
    @FXML private TableView<ScoreRow> scoreTable;

    @FXML
    public void initialize() {
        LivingBackground.attach(livingLayer, LivingBackground.Variant.PARCHMENT);

        PseudoClass winnerClass = PseudoClass.getPseudoClass("winner");
        scoreTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(ScoreRow item, boolean empty) {
                super.updateItem(item, empty);
                pseudoClassStateChanged(winnerClass, !empty && item != null && item.isWinner());
            }
        });

        if (!GameSession.hasEngine()) {
            scoreTable.setItems(FXCollections.observableArrayList());
            return;
        }

        VictoryService victoryService = new VictoryService();
        var state = GameSession.engine().getState();
        Player winner = state.getWinner().orElse(state.getCurrentPlayer());
        winnerName.setText(winner.getName());
        playTime.setText(GameSession.getSessionElapsed()
                .map(this::formatDuration)
                .orElse("--:--:--"));
        summaryLabel.setText(buildSummary(state.getLongestRoadHolder().orElse(null), state.getLargestArmyHolder().orElse(null), state.getPlayers().size()));

        ObservableList<ScoreRow> rows = FXCollections.observableArrayList(
                state.getPlayers().stream()
                        .map(player -> toScoreRow(player, winner, victoryService))
                        .sorted(Comparator
                                .comparingInt(ScoreRow::getTotal)
                                .reversed()
                                .thenComparing(ScoreRow::getName))
                        .toList()
        );
        scoreTable.setItems(rows);
    }

    @FXML
    private void onViewBoard() {
        Navigator.goTo("/fxml/game.fxml");
    }

    @FXML
    private void onMainMenu() {
        Navigator.goTo("/fxml/main_menu.fxml");
    }

    private ScoreRow toScoreRow(Player player, Player winner, VictoryService victoryService) {
        int postPoints = (int) player.getOwnedBuildings().stream()
                .filter(building -> building.getType() == BuildingType.MONITORING_POST)
                .count();
        int labPoints = (int) player.getOwnedBuildings().stream()
                .filter(building -> building.getType() == BuildingType.LABORATORY)
                .count() * 2;
        int specialPoints = player.getSpecialCards().size() * 2;
        int secretPoints = player.getSecretVictoryPoints();
        int total = victoryService.calculateVictoryPoints(player);
        String displayName = player.equals(winner) ? "🏆 " + player.getName() : player.getName();
        return new ScoreRow(displayName, postPoints, labPoints, specialPoints, secretPoints, total, player.equals(winner));
    }

    private String buildSummary(Player longestRoadHolder, Player largestArmyHolder, int playerCount) {
        String longestRoad = longestRoadHolder == null ? "none" : longestRoadHolder.getName();
        String largestArmy = largestArmyHolder == null ? "none" : largestArmyHolder.getName();
        return playerCount + " players \u00b7 Longest Road: " + longestRoad + " \u00b7 Largest Army: " + largestArmy;
    }

    private String formatDuration(Duration duration) {
        long totalSeconds = Math.max(0, duration.getSeconds());
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    public static class ScoreRow {
        private final String name;
        private final int pos;
        private final int lab;
        private final int spec;
        private final int secret;
        private final int total;
        private final boolean winner;

        public ScoreRow(String name, int pos, int lab, int spec, int secret, int total, boolean winner) {
            this.name = name;
            this.pos = pos;
            this.lab = lab;
            this.spec = spec;
            this.secret = secret;
            this.total = total;
            this.winner = winner;
        }

        public String getName() {
            return name;
        }

        public int getPos() {
            return pos;
        }

        public int getLab() {
            return lab;
        }

        public int getSpec() {
            return spec;
        }

        public int getSecret() {
            return secret;
        }

        public int getTotal() {
            return total;
        }

        public boolean isWinner() {
            return winner;
        }
    }
}
