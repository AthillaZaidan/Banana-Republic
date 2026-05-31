package com.bananarepublic.controller;

import com.bananarepublic.engine.GameEngine;
import com.bananarepublic.engine.GameState;
import com.bananarepublic.model.player.Player;
import com.bananarepublic.model.player.PlayerColor;
import com.bananarepublic.model.resource.ResourceType;
import com.bananarepublic.service.dice.DiceMode;
import com.bananarepublic.service.dice.DiceRoll;
import com.bananarepublic.service.victory.VictoryService;
import com.bananarepublic.ui.GameSession;
import com.bananarepublic.ui.HexBoard;
import com.bananarepublic.ui.LivingBackground;
import com.bananarepublic.ui.Navigator;
import com.bananarepublic.ui.PlayerBanner;
import com.bananarepublic.ui.ResourceIcons;
import com.bananarepublic.ui.WoodenFrame;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.ZoomEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import javafx.util.Duration;

import java.util.List;

public class GameController {
    @FXML private Pane livingLayer;
    @FXML private Pane frameLayer;
    @FXML private StackPane boardHolder;
    @FXML private HBox topStrip;
    @FXML private HBox resBar;
    @FXML private VBox teamList;
    @FXML private VBox logbook;
    @FXML private Label timerValue;
    @FXML private VBox timerChip;

    private int remainingSeconds = 90;
    private Timeline timer;

    private static final double BOARD_DESIGN_W = 900;
    private static final double BOARD_DESIGN_H = 780;
    private static final double BOARD_MIN_SCALE = 0.25;
    private static final double BOARD_MAX_SCALE = 3.0;
    private static final double ZOOM_FACTOR = 1.10;

    private HexBoard board;
    private Group boardCanvas;
    private final Translate canvasTranslate = new Translate();
    private final Scale canvasScale = new Scale(1, 1, 0, 0);
    private double dragStartX, dragStartY;
    private double translateStartX, translateStartY;

    @FXML
    public void initialize() {
        GameSession.setGameController(this);
        LivingBackground.attach(livingLayer, LivingBackground.Variant.OCEAN);
        board = new HexBoard(BOARD_DESIGN_W, BOARD_DESIGN_H);
        boardCanvas = new Group(board);
        boardCanvas.getTransforms().addAll(canvasTranslate, canvasScale);

        Pane canvasPane = new Pane(boardCanvas);
        canvasPane.setStyle("-fx-background-color: transparent;");
        boardHolder.getChildren().add(canvasPane);
        canvasPane.prefWidthProperty().bind(boardHolder.widthProperty());
        canvasPane.prefHeightProperty().bind(boardHolder.heightProperty());

        boardHolder.widthProperty().addListener((o, ov, nv) -> fitBoard());
        boardHolder.heightProperty().addListener((o, ov, nv) -> fitBoard());

        canvasPane.setOnScroll(this::onCanvasScroll);
        canvasPane.setOnZoom(this::onCanvasZoom);
        canvasPane.setOnMousePressed(this::onCanvasDragStart);
        canvasPane.setOnMouseDragged(this::onCanvasDragged);
        canvasPane.setCursor(javafx.scene.Cursor.DEFAULT);
        if (GameSession.hasEngine()) {
            installFromEngine();
            remainingSeconds = Math.max(0, GameSession.engine().getState().getTurnState().getRemainingSeconds());
            log("[Engine] " + GameSession.engine().getState().getCurrentPlayer().getName() + "'s turn.");
        } else {
            installPlayers();
            installTeamSidebar();
            installLog();
            installResourceBar();
        }
        installFrame();
        updateTimerDisplay();
        startTimer();
    }

    private void installFromEngine() {
        GameState state = GameSession.engine().getState();
        Player active = state.getCurrentPlayer();
        topStrip.getChildren().clear();
        teamList.getChildren().clear();
        for (Player p : state.getPlayers()) {
            boolean isActive = p == active;
            int vp = new VictoryService().calculateVictoryPoints(p);
            topStrip.getChildren().add(new PlayerBanner(
                p.getName(), cssColor(p.getColor()),
                vp, p.getTotalResourceCards(), p.getPlayedKnightCount(),
                isActive, isActive));

            addTeamRow(p.getName(), cssColor(p.getColor()),
                vp,
                p.getOwnedPipes().size(),
                ownedPosts(p),
                ownedLabs(p),
                p.getTotalResourceCards(),
                p.getHandCardCount(),
                isActive);
        }
        installResourceBarFromEngine(active);
    }

    public void refresh() {
        if (!GameSession.hasEngine()) return;
        installFromEngine();
        remainingSeconds = Math.max(0, GameSession.engine().getState().getTurnState().getRemainingSeconds());
        updateTimerDisplay();
    }

    private static int ownedPosts(Player p) {
        return (int) p.getOwnedBuildings().stream()
            .filter(b -> b.getType() == com.bananarepublic.model.building.BuildingType.MONITORING_POST)
            .count();
    }

    private static int ownedLabs(Player p) {
        return (int) p.getOwnedBuildings().stream()
            .filter(b -> b.getType() == com.bananarepublic.model.building.BuildingType.LABORATORY)
            .count();
    }

    private static String cssColor(PlayerColor c) {
        return switch (c) {
            case RED    -> "red";
            case BLUE   -> "blue";
            case YELLOW -> "gold";
            case GREEN  -> "white";
        };
    }

    public void log(String entry) {
        HBox row = new HBox(6);
        row.getStyleClass().add("log-entry");
        Label text = new Label(entry);
        text.getStyleClass().add("log-text");
        row.getChildren().add(text);
        logbook.getChildren().add(0, row);
    }

    private void installResourceBar() {
        resBar.getChildren().clear();
        StackPane vp = new StackPane(new Label("5"));
        vp.getStyleClass().add("res-chip-vp");
        resBar.getChildren().add(vp);

        resBar.getChildren().add(resChip(ResourceIcons.Kind.WOOD,   2));
        resBar.getChildren().add(resChip(ResourceIcons.Kind.BRICK,  1));
        resBar.getChildren().add(resChip(ResourceIcons.Kind.WHEAT,  2));
        resBar.getChildren().add(resChip(ResourceIcons.Kind.ORE,    1));
        resBar.getChildren().add(resChip(ResourceIcons.Kind.BANANA, 1));
    }

    private void installResourceBarFromEngine(Player active) {
        resBar.getChildren().clear();
        int vp = new VictoryService().calculateVictoryPoints(active);
        StackPane vpNode = new StackPane(new Label(String.valueOf(vp)));
        vpNode.getStyleClass().add("res-chip-vp");
        resBar.getChildren().add(vpNode);

        resBar.getChildren().add(resChip(ResourceIcons.Kind.WOOD,   active.getResourceAmount(ResourceType.WOOD)));
        resBar.getChildren().add(resChip(ResourceIcons.Kind.BRICK,  active.getResourceAmount(ResourceType.BRICK)));
        resBar.getChildren().add(resChip(ResourceIcons.Kind.WHEAT,  active.getResourceAmount(ResourceType.WHEAT)));
        resBar.getChildren().add(resChip(ResourceIcons.Kind.ORE,    active.getResourceAmount(ResourceType.ORE)));
        resBar.getChildren().add(resChip(ResourceIcons.Kind.BANANA, active.getResourceAmount(ResourceType.BANANA)));
    }

    private HBox resChip(ResourceIcons.Kind kind, int count) {
        HBox chip = new HBox(6);
        chip.getStyleClass().add("res-chip");
        chip.setAlignment(Pos.CENTER_LEFT);
        StackPane iconSlot = new StackPane(ResourceIcons.of(kind));
        iconSlot.setMinSize(22, 22); iconSlot.setMaxSize(22, 22);
        Label countLabel = new Label(String.valueOf(count));
        chip.getChildren().addAll(iconSlot, countLabel);
        return chip;
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

    private void fitBoard() {
        if (board == null) return;
        double availW = boardHolder.getWidth()
            - boardHolder.getPadding().getLeft() - boardHolder.getPadding().getRight();
        double availH = boardHolder.getHeight()
            - boardHolder.getPadding().getTop() - boardHolder.getPadding().getBottom();
        if (availW <= 0 || availH <= 0) return;
        double scale = Math.min(availW / BOARD_DESIGN_W, availH / BOARD_DESIGN_H);
        scale = Math.max(BOARD_MIN_SCALE, Math.min(scale, BOARD_MAX_SCALE));
        canvasScale.setX(scale);
        canvasScale.setY(scale);
        canvasTranslate.setX((availW - BOARD_DESIGN_W * scale) / 2
            + boardHolder.getPadding().getLeft());
        canvasTranslate.setY((availH - BOARD_DESIGN_H * scale) / 2
            + boardHolder.getPadding().getTop());
    }

    private void applyZoom(double factor, double pivotX, double pivotY) {
        double oldScale = canvasScale.getX();
        double newScale = Math.max(BOARD_MIN_SCALE, Math.min(oldScale * factor, BOARD_MAX_SCALE));
        double ratio = newScale / oldScale;
        canvasTranslate.setX(pivotX - ratio * (pivotX - canvasTranslate.getX()));
        canvasTranslate.setY(pivotY - ratio * (pivotY - canvasTranslate.getY()));
        canvasScale.setX(newScale);
        canvasScale.setY(newScale);
    }

    private void onCanvasScroll(ScrollEvent e) {
        if (e.getTouchCount() > 0) return; // handled by onCanvasZoom (trackpad pinch)
        double factor = e.getDeltaY() > 0 ? ZOOM_FACTOR : 1.0 / ZOOM_FACTOR;
        applyZoom(factor, e.getX(), e.getY());
        e.consume();
    }

    private void onCanvasZoom(ZoomEvent e) {
        applyZoom(e.getZoomFactor(), e.getX(), e.getY());
        e.consume();
    }

    private void onCanvasDragStart(MouseEvent e) {
        if (e.getButton() == MouseButton.PRIMARY || e.getButton() == MouseButton.MIDDLE) {
            dragStartX = e.getSceneX();
            dragStartY = e.getSceneY();
            translateStartX = canvasTranslate.getX();
            translateStartY = canvasTranslate.getY();
            ((Pane) e.getSource()).setCursor(javafx.scene.Cursor.CLOSED_HAND);
        }
    }

    private void onCanvasDragged(MouseEvent e) {
        if (e.getButton() == MouseButton.PRIMARY || e.getButton() == MouseButton.MIDDLE) {
            canvasTranslate.setX(translateStartX + (e.getSceneX() - dragStartX));
            canvasTranslate.setY(translateStartY + (e.getSceneY() - dragStartY));
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
        if (GameSession.hasEngine()) {
            GameSession.engine().getState().getTurnState().setRemainingSeconds(remainingSeconds);
        }
        updateTimerDisplay();
    }

    private void updateTimerDisplay() {
        int mm = remainingSeconds / 60;
        int ss = remainingSeconds % 60;
        timerValue.setText(String.format("%02d:%02d", mm, ss));
        if (remainingSeconds <= 10) {
            if (!timerChip.getStyleClass().contains("is-urgent")) {
                timerChip.getStyleClass().add("is-urgent");
            }
        } else {
            timerChip.getStyleClass().remove("is-urgent");
        }
    }

    @FXML private void onScoreboard() { System.out.println("[Game] scoreboard"); }
    @FXML private void onTrade()      { Navigator.showOverlay("/fxml/trade_dialog.fxml"); }
    @FXML private void onCards()      { Navigator.showOverlay("/fxml/cards_dialog.fxml"); }
    @FXML private void onSettings()   { Navigator.showOverlay("/fxml/settings_dialog.fxml"); }
    @FXML
    private void onRollDice() {
        if (!GameSession.hasEngine()) {
            System.out.println("[Game] roll dice (no engine)");
            return;
        }
        GameEngine engine = GameSession.engine();
        try {
            DiceRoll roll = engine.rollDice(DiceMode.RANDOM, null);
            log("[Roll] " + engine.getState().getCurrentPlayer().getName()
                + " rolled " + roll.getFirst() + "+" + roll.getSecond()
                + " = " + roll.total() + ".");
            if (roll.total() == 7 && engine.getState().getTurnState().getPhase() == com.bananarepublic.engine.TurnPhase.DISCARD) {
                Navigator.showOverlay("/fxml/discard_dialog.fxml");
            }
            checkVictory();
        } catch (RuntimeException ex) {
            log("[Roll] " + ex.getMessage());
        }
    }

    private void checkVictory() {
        if (!GameSession.hasEngine()) return;
        if (GameSession.engine().getState().isGameOver()) {
            Navigator.showOverlay("/fxml/victory_dialog.fxml");
        }
    }

    @FXML
    private void onEndTurn() {
        if (timer != null) timer.stop();
        if (GameSession.hasEngine()) {
            GameEngine engine = GameSession.engine();
            try {
                if (engine.getState().isGameOver()) {
                    Navigator.showOverlay("/fxml/victory_dialog.fxml");
                    return;
                }
                engine.endTurn();
            } catch (RuntimeException ex) {
                System.out.println("[EndTurn] " + ex.getMessage());
            }
        }
        Navigator.goTo("/fxml/turn_transition.fxml");
    }
}
