package com.bananarepublic.controller;

import com.bananarepublic.model.building.BuildingType;
import com.bananarepublic.model.card.DevelopmentCard;
import com.bananarepublic.model.card.VictoryPointCard;
import com.bananarepublic.model.player.Player;
import com.bananarepublic.model.player.SpecialCardType;
import com.bananarepublic.ui.GameSession;
import com.bananarepublic.ui.AudioEngine;
import com.bananarepublic.ui.Navigator;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;

public class VictoryDialogController {
    private record Card(String kind, String name, String type, String value) {}

    @FXML private StackPane root;
    @FXML private Label publicPointsValue;
    @FXML private Label publicPointsBreakdown;
    @FXML private Label secretPointsValue;
    @FXML private Label secretPointsBreakdown;
    @FXML private VBox secretList;

    @FXML
    public void initialize() {
        if (!GameSession.hasEngine()) {
            return;
        }

        Player winner = GameSession.engine().getState().getWinner()
                .orElse(GameSession.engine().getState().getCurrentPlayer());
        int monitoringPosts = (int) winner.getOwnedBuildings().stream()
                .filter(building -> building.getType() == BuildingType.MONITORING_POST)
                .count();
        int laboratories = (int) winner.getOwnedBuildings().stream()
                .filter(building -> building.getType() == BuildingType.LABORATORY)
                .count();
        int specialPoints = winner.getSpecialCards().size() * 2;
        int publicPoints = monitoringPosts + laboratories * 2 + specialPoints;
        int secretPoints = winner.getSecretVictoryPoints();

        publicPointsValue.setText(String.valueOf(publicPoints));
        publicPointsBreakdown.setText(monitoringPosts + " post(s) · "
                + laboratories + " lab(s) · "
                + describeSpecialCards(winner.getSpecialCards()));
        secretPointsValue.setText("+" + secretPoints);

        List<DevelopmentCard> revealedCards = winner.getHandCards().stream()
                .filter(VictoryPointCard.class::isInstance)
                .toList();
        secretPointsBreakdown.setText(revealedCards.size() + " revealed prestige card(s)");

        secretList.getChildren().clear();
        if (revealedCards.isEmpty()) {
            secretList.getChildren().add(buildRow(new Card("vp", "No hidden prestige cards", "Victory Point Card", "+0 VP")));
            return;
        }

        for (DevelopmentCard card : revealedCards) {
            secretList.getChildren().add(buildRow(new Card(
                    "vp",
                    card.getName(),
                    card.getClass().getSimpleName(),
                    "+1 VP"
            )));
        }
    }

    private String describeSpecialCards(java.util.Set<SpecialCardType> specialCards) {
        if (specialCards.isEmpty()) {
            return "no bonus cards";
        }
        return specialCards.stream()
                .map(card -> switch (card) {
                    case LONGEST_ROAD -> "Longest Road";
                    case LARGEST_ARMY -> "Largest Army";
                })
                .sorted()
                .reduce((left, right) -> left + " · " + right)
                .orElse("no bonus cards");
    }

    private HBox buildRow(Card c) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-padding: 10 12; -fx-background-radius: 10;"
                + " -fx-background-color: rgba(255,255,255,0.55);"
                + " -fx-border-color: -parchment-line; -fx-border-radius: 10; -fx-border-width: 1;");

        StackPane swatch = new StackPane();
        swatch.setMinSize(28, 28);
        swatch.setMaxSize(28, 28);
        swatch.setStyle("-fx-background-radius: 6;"
                + (c.kind().equals("vp")
                ? "-fx-background-color: -gold-1;"
                : "-fx-background-color: #7e3fb8;"));
        Label icon = new Label(c.kind().equals("vp") ? "VP" : "KNT");
        icon.setStyle("-fx-text-fill: white;");
        swatch.getChildren().add(icon);

        VBox info = new VBox(2);
        Label name = new Label(c.name());
        name.setStyle("-fx-font-weight: 800; -fx-font-size: 13px;");
        Label type = new Label(c.type());
        type.setStyle("-fx-font-size: 11px; -fx-text-fill: -ink-mute;");
        info.getChildren().addAll(name, type);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label val = new Label(c.value());
        val.setStyle("-fx-padding: 4 10; -fx-background-radius: 999;"
                + " -fx-font-size: 11px; -fx-font-weight: 800;"
                + (c.kind().equals("vp")
                ? "-fx-background-color: -gold-1; -fx-text-fill: -gold-deep;"
                : "-fx-background-color: rgba(0,0,0,0.08); -fx-text-fill: -ink-mute;"));

        row.getChildren().addAll(swatch, info, val);
        return row;
    }

    @FXML
    private void onConfirm() {
        AudioEngine.get().playSfx(AudioEngine.Sfx.CLICK);
        Navigator.closeOverlay(root);
        Navigator.goTo("/fxml/game_result.fxml");
    }

    @FXML
    private void onClose() {
        AudioEngine.get().playSfx(AudioEngine.Sfx.CLICK);
        close();
    }

    private void close() {
        Navigator.closeOverlay(root);
    }
}
