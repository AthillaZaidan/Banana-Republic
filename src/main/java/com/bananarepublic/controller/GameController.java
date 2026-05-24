package com.bananarepublic.controller;

import com.bananarepublic.ui.HexBoard;
import com.bananarepublic.ui.LivingBackground;
import com.bananarepublic.ui.Navigator;
import com.bananarepublic.ui.PlayerBanner;
import com.bananarepublic.ui.WoodenFrame;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.List;

public class GameController {
    @FXML private Pane livingLayer;
    @FXML private Pane frameLayer;
    @FXML private StackPane boardHolder;
    @FXML private HBox topStrip;
    @FXML private VBox teamList;
    @FXML private VBox logbook;
    @FXML private Label timerValue;
    @FXML private VBox timerChip;

    private int remainingSeconds = 90;
    private Timeline timer;

    @FXML
    public void initialize() {
        LivingBackground.attach(livingLayer, LivingBackground.Variant.OCEAN);
        boardHolder.getChildren().add(new HexBoard(720, 600));
        installPlayers();
        installTeamSidebar();
        installLog();
        installFrame();
        startTimer();
    }

    private void installPlayers() {
        topStrip.getChildren().add(new PlayerBanner("Stewart", "red",   2, 1, 0, false, false));
        topStrip.getChildren().add(new PlayerBanner("Gro",     "blue",  5, 7, 1, true,  true));
        topStrip.getChildren().add(new PlayerBanner("Kebin",   "gold",  3, 2, 1, false, false));
        topStrip.getChildren().add(new PlayerBanner("Tara",    "white", 4, 3, 0, false, false));
    }

    private void installTeamSidebar() {
        addTeamRow("Stewart", "red",   2, 4, 2, 0, 3, 0, false);
        addTeamRow("Gro",     "blue",  5, 8, 3, 1, 7, 2, true);
        addTeamRow("Kebin",   "gold",  3, 6, 2, 1, 2, 1, false);
        addTeamRow("Tara",    "white", 4, 5, 3, 1, 3, 0, false);
    }

    private void addTeamRow(String name, String color, int vp,
                            int pipe, int post, int lab, int cards, int dev,
                            boolean active) {
        VBox row = new VBox(6);
        row.getStyleClass().add("card-dark");
        row.setStyle("-fx-padding: 8 10 10 10;"
            + (active ? "-fx-border-color: -p-" + color + "; -fx-border-width: 0 0 0 4;"
                      : "-fx-border-width: 0 0 0 4; -fx-border-color: transparent;"));

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        StackPane chip = new StackPane();
        chip.getStyleClass().addAll("initial-chip", "pc-" + color);
        chip.setMinSize(28, 28); chip.setMaxSize(28, 28);
        Label letter = new Label(String.valueOf(name.charAt(0)));
        letter.setStyle("-fx-font-size: 13px;");
        chip.getChildren().add(letter);

        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: -ink-on-dark;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        StackPane coin = new StackPane(new Label(String.valueOf(vp)));
        coin.getStyleClass().add("coin");
        coin.setMinSize(26, 26); coin.setMaxSize(26, 26);

        header.getChildren().addAll(chip, nameLabel, spacer, coin);

        HBox stats = new HBox(8);
        stats.setAlignment(Pos.CENTER_LEFT);
        stats.getChildren().addAll(
            statCell("🛢", pipe + "/15"),
            statCell("🛡", post + "/5"),
            statCell("🔬", lab + "/4"),
            statCell("🃏", String.valueOf(cards)),
            statCell("📜", String.valueOf(dev))
        );

        row.getChildren().addAll(header, stats);
        teamList.getChildren().add(row);
    }

    private VBox statCell(String icon, String value) {
        VBox cell = new VBox(2);
        cell.setAlignment(Pos.CENTER);
        Label i = new Label(icon);
        i.setStyle("-fx-font-size: 12px;");
        Label v = new Label(value);
        v.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #f1e6cf;");
        cell.getChildren().addAll(i, v);
        HBox.setHgrow(cell, Priority.ALWAYS);
        return cell;
    }

    private void installLog() {
        List<String[]> entries = List.of(
            new String[]{"Gro",     "blue",  "rolled an 8.",                "NEW"},
            new String[]{"You",     "blue",  "received 2 Wheat, 1 Wood.",   ""},
            new String[]{"Stewart", "red",   "received 1 Brick.",           ""},
            new String[]{"Kebin",   "gold",  "built a Pipe on F-G.",        ""},
            new String[]{"Tara",    "white", "offered Trade: 2 🍌 ↔ 1 ⛏.", ""},
            new String[]{"You",     "blue",  "declined trade.",             ""},
            new String[]{"Stewart", "red",   "rolled a 6.",                 ""},
            new String[]{"Tara",    "white", "received 1 Wood.",            ""},
            new String[]{"Gro",     "blue",  "bought an Experiment Card.",  ""}
        );
        int i = 0;
        for (String[] e : entries) {
            HBox row = new HBox(6);
            row.getStyleClass().add("log-entry");
            row.setOpacity(1.0 - i * 0.06);
            Label who = new Label(e[0]);
            who.getStyleClass().add("log-who");
            who.setStyle("-fx-text-fill: -p-" + e[1] + ";");
            Label text = new Label(e[2]);
            text.getStyleClass().add("log-text");
            HBox.setHgrow(text, Priority.ALWAYS);
            text.setMaxWidth(Double.MAX_VALUE);
            row.getChildren().addAll(who, text);
            if (!e[3].isEmpty()) {
                Label tag = new Label(e[3]);
                tag.setStyle("-fx-text-fill: -gold-1; -fx-font-size: 9px; -fx-font-weight: 800;");
                row.getChildren().add(tag);
            }
            logbook.getChildren().add(row);
            i++;
        }
    }

    private void installFrame() {
        frameLayer.widthProperty().addListener((obs, oldW, newW) -> rebuildFrame());
        frameLayer.heightProperty().addListener((obs, oldH, newH) -> rebuildFrame());
        rebuildFrame();
    }

    private void rebuildFrame() {
        frameLayer.getChildren().clear();
        double w = frameLayer.getWidth();
        double h = frameLayer.getHeight();
        if (w > 0 && h > 0) {
            Group frame = WoodenFrame.build(w, h);
            frameLayer.getChildren().add(frame);
        }
    }

    private void startTimer() {
        timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> tickTimer()));
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();
    }

    private void tickTimer() {
        if (remainingSeconds <= 0) {
            timer.stop();
            return;
        }
        remainingSeconds--;
        int mm = remainingSeconds / 60;
        int ss = remainingSeconds % 60;
        timerValue.setText(String.format("%02d:%02d", mm, ss));
        if (remainingSeconds <= 10 && !timerChip.getStyleClass().contains("is-urgent")) {
            timerChip.getStyleClass().add("is-urgent");
        }
    }

    @FXML private void onScoreboard() { System.out.println("[Game] scoreboard"); }
    @FXML private void onTrade()      { Navigator.openModal("/fxml/trade_dialog.fxml",    "Trade Resources"); }
    @FXML private void onCards()      { Navigator.openModal("/fxml/cards_dialog.fxml",    "Experiment Cards"); }
    @FXML private void onSettings()   { Navigator.openModal("/fxml/settings_dialog.fxml", "Settings & Plugins"); }
    @FXML private void onRollDice()   { System.out.println("[Game] roll dice"); }
    @FXML
    private void onEndTurn() {
        if (timer != null) timer.stop();
        Navigator.goTo("/fxml/turn_transition.fxml");
    }
}
