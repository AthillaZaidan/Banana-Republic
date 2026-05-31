package com.bananarepublic.controller;

import com.bananarepublic.engine.BoardMode;
import com.bananarepublic.engine.GameConfig;
import com.bananarepublic.engine.GameEngine;
import com.bananarepublic.engine.GameState;
import com.bananarepublic.engine.PlayerConfig;
import com.bananarepublic.engine.TurnPhase;
import com.bananarepublic.model.board.HexTile;
import com.bananarepublic.model.board.Path;
import com.bananarepublic.model.player.Player;
import com.bananarepublic.model.player.PlayerColor;
import com.bananarepublic.model.resource.ResourceInventory;
import com.bananarepublic.model.resource.ResourceType;
import com.bananarepublic.service.build.BuildActionType;
import com.bananarepublic.service.build.BuildCostProvider;
import com.bananarepublic.service.dice.DiceMode;
import com.bananarepublic.service.dice.DiceRoll;
import com.bananarepublic.service.victory.VictoryService;
import com.bananarepublic.ui.DiceDialogRequest;
import com.bananarepublic.ui.DiceDialogResult;
import com.bananarepublic.ui.DicePips;
import com.bananarepublic.ui.AudioEngine;
import com.bananarepublic.ui.GameSession;
import com.bananarepublic.ui.HexBoard;
import com.bananarepublic.ui.LivingBackground;
import com.bananarepublic.ui.Navigator;
import com.bananarepublic.ui.ResourceIcons;
import com.bananarepublic.ui.WoodenFrame;
import javafx.application.Platform;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.ZoomEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.shape.StrokeType;
import javafx.scene.paint.Color;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class GameController {
    private enum BoardSelectionType { INTERSECTION, PATH, TILE }
    private record PendingBoardSelection(
            BoardSelectionType type,
            List<String> ids,
            String prompt,
            Consumer<String> onSelect,
            Runnable onCancel
    ) {}

    @FXML private Pane livingLayer;
    @FXML private Pane frameLayer;
    @FXML private StackPane boardHolder;
    @FXML private AnchorPane hud;
    @FXML private VBox leftToolbox;
    @FXML private HBox topStrip;
    @FXML private HBox resBar;
    @FXML private VBox teamList;
    @FXML private VBox logbook;
    @FXML private ScrollPane logScroll;
    @FXML private Label timerValue;
    @FXML private VBox timerChip;
    @FXML private Label phaseLabel;
    @FXML private Label diceSummaryLabel;
    @FXML private Pane dieOneValue;
    @FXML private Pane dieTwoValue;
    @FXML private Button scoreboardBtn;
    @FXML private Button tradeBtn;
    @FXML private Button cardsBtn;
    @FXML private Button buildCostsBtn;
    @FXML private Button settingsBtn;
    @FXML private StackPane buildPostHintZone;
    @FXML private StackPane buildPipeHintZone;
    @FXML private StackPane upgradeLabHintZone;
    @FXML private StackPane resolveNimonHintZone;
    @FXML private StackPane sidebarHoverPopup;
    @FXML private Button buildPostBtn;
    @FXML private Button buildPipeBtn;
    @FXML private Button upgradeLabBtn;
    @FXML private Button resolveNimonBtn;
    @FXML private Button rollDiceBtn;
    @FXML private Button endTurnBtn;

    private Timeline uiTimer;

    private static final double BOARD_DESIGN_W = 900;
    private static final double BOARD_DESIGN_H = 780;
    private static final double BOARD_MIN_SCALE = 0.25;
    private static final double BOARD_MAX_SCALE = 3.0;
    private static final double ZOOM_FACTOR = 1.10;

    private HexBoard board;
    private Group boardCanvas;
    private Pane boardSelectionLayer;
    private final BuildCostProvider buildCostProvider = new BuildCostProvider();
    private final Translate canvasTranslate = new Translate();
    private final Scale canvasScale = new Scale(1, 1, 0, 0);
    private final Random uiRandom = new Random();
    private double dragStartX;
    private double dragStartY;
    private double translateStartX;
    private double translateStartY;
    private String trackedPlayerId;
    private TurnPhase trackedPhase;
    private Timeline diceAnimation;
    private boolean diceAnimationRunning;
    private boolean botAutomationQueued;
    private DiceFlowContext diceFlowContext;
    private Node sidebarHoverAnchor;
    private GameConfig startingOrderBaseConfig;
    private List<PlayerConfig> startingOrderOriginalOrder = List.of();
    private List<PlayerConfig> startingOrderContenders = List.of();
    private final Map<PlayerConfig, DiceRoll> startingOrderRoundRolls = new LinkedHashMap<>();
    private int startingOrderRollIndex;
    private PendingBoardSelection pendingBoardSelection;

    @FXML
    public void initialize() {
        GameSession.setGameController(this);
        LivingBackground.attach(livingLayer, LivingBackground.Variant.OCEAN);
        AudioEngine.get().playGameBgm();
        configureSidebarHoverZones();
        installSidebarHoverCards();

        board = new HexBoard(GameSession.hasEngine() ? GameSession.engine().getState() : null, BOARD_DESIGN_W, BOARD_DESIGN_H);
        boardSelectionLayer = new Pane();
        boardSelectionLayer.setPickOnBounds(false);
        boardSelectionLayer.setMouseTransparent(false);
        boardSelectionLayer.setPrefSize(BOARD_DESIGN_W, BOARD_DESIGN_H);
        boardCanvas = new Group(board, boardSelectionLayer);
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
            GameState state = GameSession.engine().getState();
            installFromEngine(state);
            trackedPlayerId = state.getCurrentPlayer().getId();
            trackedPhase = state.getTurnState().getPhase();
            if (GameSession.isStartingOrderPending()) {
                updateDiceDisplay(null, "Press the dice button to determine the first player.");
                log("[Setup] Determine the first player from inside the map.");
            } else {
                log("[Turn] " + state.getCurrentPlayer().getName() + " is active.");
            }
        } else {
            installPreviewData();
        }

        installFrame();
        fitBoard();
        syncTimerAndPhaseUi();
        startUiTimer();
        queueBotAutomationIfNeeded();
    }

    public void refresh() {
        if (!GameSession.hasEngine()) {
            return;
        }

        GameState state = GameSession.engine().getState();
        installFromEngine(state);
        syncTimerAndPhaseUi();
        trackedPlayerId = state.getCurrentPlayer().getId();
        trackedPhase = state.getTurnState().getPhase();
        queueBotAutomationIfNeeded();
    }

    public void continueSpecialTurnFlow() {
        if (!GameSession.hasEngine()) {
            return;
        }

        TurnPhase phase = GameSession.engine().getState().getTurnState().getPhase();
        try {
            if (phase == TurnPhase.DISCARD) {
                Navigator.showOverlay("/fxml/discard_dialog.fxml");
            } else if (phase == TurnPhase.MOVE_NIMON_UNGU) {
                promptNimonFlow();
            }
        } catch (RuntimeException ex) {
            showError("Special Phase Failed", ex.getMessage());
        }
        refresh();
    }

    public void log(String entry) {
        HBox row = new HBox(6);
        row.getStyleClass().add("log-entry");
        row.setAlignment(Pos.TOP_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);
        Label text = new Label(entry);
        text.getStyleClass().add("log-text");
        text.setWrapText(true);
        text.setMaxWidth(Double.MAX_VALUE);
        text.prefWidthProperty().bind(row.widthProperty().subtract(2));
        HBox.setHgrow(text, Priority.ALWAYS);
        row.getChildren().add(text);
        logbook.getChildren().add(row);
        Platform.runLater(() -> {
            if (logScroll != null) {
                logScroll.setVvalue(1.0);
            }
        });
    }

    @FXML
    private void onScoreboard() {
        if (isBoardSelectionActive()) {
            showInfo("Placement Active", "Finish the current map placement first.");
            return;
        }
        AudioEngine.get().playSfx(AudioEngine.Sfx.CLICK);
        if (!GameSession.hasEngine()) {
            return;
        }
        Navigator.showOverlay("/fxml/scoreboard_dialog.fxml");
    }

    @FXML
    private void onTrade() {
        if (isBoardSelectionActive()) {
            showInfo("Placement Active", "Finish the current map placement first.");
            return;
        }
        AudioEngine.get().playSfx(AudioEngine.Sfx.CLICK);
        Navigator.showOverlay("/fxml/trade_dialog.fxml");
    }

    @FXML
    private void onCards() {
        if (isBoardSelectionActive()) {
            showInfo("Placement Active", "Finish the current map placement first.");
            return;
        }
        AudioEngine.get().playSfx(AudioEngine.Sfx.CLICK);
        Navigator.showOverlay("/fxml/cards_dialog.fxml");
    }

    @FXML
    private void onBuildCosts() {
        if (isBoardSelectionActive()) {
            showInfo("Placement Active", "Finish the current map placement first.");
            return;
        }
        Navigator.showOverlay("/fxml/build_costs_dialog.fxml");
    }

    @FXML
    private void onSettings() {
        if (isBoardSelectionActive()) {
            showInfo("Placement Active", "Finish the current map placement first.");
            return;
        }
        AudioEngine.get().playSfx(AudioEngine.Sfx.CLICK);
        Navigator.showOverlay("/fxml/settings_dialog.fxml");
    }

    @FXML
    private void onBuildPost() {
        if (isBoardSelectionActive()) {
            showInfo("Placement Active", "Finish the current map placement first.");
            return;
        }
        if (!GameSession.hasEngine()) {
            return;
        }

        GameEngine engine = GameSession.engine();
        GameState state = engine.getState();
        String playerId = state.getCurrentPlayer().getId();
        TurnPhase phase = state.getTurnState().getPhase();

        try {
            if (phase == TurnPhase.SETUP) {
                if (state.getTurnState().isWaitingForSetupPipe()) {
                    showInfo("Setup", "Place the setup pipe before placing another monitoring post.");
                    return;
                }

                if (!beginIntersectionSelection(
                        "Click a highlighted intersection to place the setup monitoring post.",
                        engine.getValidSetupPostIds(playerId),
                        intersectionId -> {
                            try {
                                GameEngine currentEngine = GameSession.engine();
                                GameState currentState = currentEngine.getState();
                                currentEngine.placeSetupWatchPost(playerId, intersectionId);
                                AudioEngine.get().playSfx(AudioEngine.Sfx.BUILD);
                                log("[Setup] " + currentState.getCurrentPlayer().getName()
                                        + " placed a monitoring post at " + intersectionId + ".");
                                refresh();
                            } catch (RuntimeException ex) {
                                showError("Build Failed", ex.getMessage());
                                refresh();
                            }
                        }
                )) {
                    return;
                }
            } else if (phase == TurnPhase.TRADE_BUILD) {
                if (!beginIntersectionSelection(
                        "Click a highlighted intersection to build a monitoring post.",
                        engine.getValidMonitoringPostIds(playerId),
                        intersectionId -> {
                            try {
                                GameEngine currentEngine = GameSession.engine();
                                String playerName = currentEngine.getState().getCurrentPlayer().getName();
                                currentEngine.buildWatchPost(playerId, intersectionId);
                                AudioEngine.get().playSfx(AudioEngine.Sfx.BUILD);
                                log("[Build] " + playerName + " built a monitoring post at " + intersectionId + ".");
                                checkVictory();
                                refresh();
                            } catch (RuntimeException ex) {
                                showError("Build Failed", ex.getMessage());
                                refresh();
                            }
                        }
                )) {
                    return;
                }
            } else {
                showInfo("Unavailable", "Monitoring posts can only be placed during setup or trade/build.");
                return;
            }
        } catch (RuntimeException ex) {
            showError("Build Failed", ex.getMessage());
        }
    }

    @FXML
    private void onBuildPipe() {
        if (isBoardSelectionActive()) {
            showInfo("Placement Active", "Finish the current map placement first.");
            return;
        }
        if (!GameSession.hasEngine()) {
            return;
        }

        GameEngine engine = GameSession.engine();
        GameState state = engine.getState();
        String playerId = state.getCurrentPlayer().getId();
        TurnPhase phase = state.getTurnState().getPhase();

        try {
            if (phase == TurnPhase.SETUP) {
                if (!state.getTurnState().isWaitingForSetupPipe()) {
                    showInfo("Setup", "Place a setup monitoring post first.");
                    return;
                }

                if (!beginPathSelection(
                        "Click a highlighted path to place the setup pipe.",
                        engine.getValidSetupRoadIds(playerId),
                        pathId -> {
                            try {
                                GameEngine currentEngine = GameSession.engine();
                                String playerName = currentEngine.getState().getCurrentPlayer().getName();
                                currentEngine.placeSetupRoad(playerId, pathId);
                                AudioEngine.get().playSfx(AudioEngine.Sfx.BUILD);
                                log("[Setup] " + playerName + " placed a pipe on " + pathId + ".");
                                if (currentEngine.getState().getTurnState().getPhase() == TurnPhase.RESOURCE_GATHERING) {
                                    log("[Setup] Initial placement complete. Roll dice to begin the match.");
                                }
                                refresh();
                            } catch (RuntimeException ex) {
                                showError("Build Failed", ex.getMessage());
                                refresh();
                            }
                        }
                )) {
                    return;
                }
            } else if (phase == TurnPhase.TRADE_BUILD) {
                if (!beginPathSelection(
                        "Click a highlighted path to build a pipe.",
                        engine.getValidRoadIds(playerId),
                        pathId -> {
                            try {
                                GameEngine currentEngine = GameSession.engine();
                                String playerName = currentEngine.getState().getCurrentPlayer().getName();
                                currentEngine.buildRoad(playerId, pathId);
                                AudioEngine.get().playSfx(AudioEngine.Sfx.BUILD);
                                log("[Build] " + playerName + " built a pipe on " + pathId + ".");
                                checkVictory();
                                refresh();
                            } catch (RuntimeException ex) {
                                showError("Build Failed", ex.getMessage());
                                refresh();
                            }
                        }
                )) {
                    return;
                }
            } else {
                showInfo("Unavailable", "Pipes can only be placed during setup or trade/build.");
                return;
            }
        } catch (RuntimeException ex) {
            showError("Build Failed", ex.getMessage());
        }
    }

    @FXML
    private void onUpgradeLab() {
        if (isBoardSelectionActive()) {
            showInfo("Placement Active", "Finish the current map placement first.");
            return;
        }
        if (!GameSession.hasEngine()) {
            return;
        }

        GameEngine engine = GameSession.engine();
        GameState state = engine.getState();
        if (state.getTurnState().getPhase() != TurnPhase.TRADE_BUILD) {
            showInfo("Unavailable", "Laboratories can only be upgraded during trade/build.");
            return;
        }

        try {
            if (!beginIntersectionSelection(
                    "Click one of your highlighted monitoring posts to upgrade it.",
                    engine.getValidLaboratoryUpgradeIds(state.getCurrentPlayer().getId()),
                    intersectionId -> {
                        try {
                            GameEngine currentEngine = GameSession.engine();
                            String playerName = currentEngine.getState().getCurrentPlayer().getName();
                            currentEngine.upgradeLaboratory(currentEngine.getState().getCurrentPlayer().getId(), intersectionId);
                            AudioEngine.get().playSfx(AudioEngine.Sfx.BUILD);
                            log("[Build] " + playerName + " upgraded " + intersectionId + " into a laboratory.");
                            checkVictory();
                            refresh();
                        } catch (RuntimeException ex) {
                            showError("Upgrade Failed", ex.getMessage());
                            refresh();
                        }
                    }
            )) {
                return;
            }
        } catch (RuntimeException ex) {
            showError("Upgrade Failed", ex.getMessage());
        }
    }

    @FXML
    private void onResolveNimon() {
        if (isBoardSelectionActive()) {
            showInfo("Placement Active", "Finish the current map placement first.");
            return;
        }
        AudioEngine.get().playSfx(AudioEngine.Sfx.CLICK);
        if (!GameSession.hasEngine()) {
            return;
        }

        try {
            promptNimonFlow();
        } catch (RuntimeException ex) {
            showError("Nimon Flow Failed", ex.getMessage());
        }
        refresh();
    }

    @FXML
    private void onRollDice() {
        if (isBoardSelectionActive()) {
            showInfo("Placement Active", "Finish the current map placement first.");
            return;
        }
        if (!GameSession.hasEngine()) {
            return;
        }
        if (diceAnimationRunning || diceFlowContext != null) {
            return;
        }

        if (GameSession.isStartingOrderPending()) {
            beginStartingOrderFlow();
            return;
        }

        GameEngine engine = GameSession.engine();
        GameState state = engine.getState();
        if (state.getTurnState().getPhase() != TurnPhase.RESOURCE_GATHERING) {
            showInfo("Unavailable", "Dice can only be rolled at the start of a turn.");
            return;
        }

        diceFlowContext = DiceFlowContext.NORMAL_TURN;
        showDiceDialog(
                "ROLL DICE",
                "Choose random roll or pick both dice manually.",
                "ROLL DICE",
                engine.isManualDiceEnabled()
        );
    }

    public void onDiceDialogResolved(DiceDialogResult result) {
        DiceFlowContext resolvedContext = diceFlowContext;
        diceFlowContext = null;

        if (result == null) {
            if (resolvedContext == DiceFlowContext.STARTING_ORDER) {
                resetStartingOrderFlow();
                updateDiceDisplay(null, "Starting order not resolved.");
                refresh();
            }
            return;
        }

        if (resolvedContext == DiceFlowContext.STARTING_ORDER) {
            resolveStartingOrderRoll(result);
            return;
        }
        if (resolvedContext == DiceFlowContext.NORMAL_TURN) {
            resolveNormalTurnRoll(result);
        }
    }

    @FXML
    private void onEndTurn() {
        if (isBoardSelectionActive()) {
            showInfo("Placement Active", "Finish the current map placement first.");
            return;
        }
        AudioEngine.get().playSfx(AudioEngine.Sfx.CLICK);
        if (!GameSession.hasEngine()) {
            return;
        }

        GameEngine engine = GameSession.engine();
        try {
            if (engine.getState().isGameOver()) {
                Navigator.showOverlay("/fxml/victory_dialog.fxml");
                return;
            }
            engine.endTurn();
        } catch (RuntimeException ex) {
            showError("End Turn Failed", ex.getMessage());
            return;
        }

        if (uiTimer != null) {
            uiTimer.stop();
        }
        Navigator.goTo("/fxml/turn_transition.fxml");
    }

    private void installFromEngine(GameState state) {
        board.render(state);
        renderBoardSelection();
        teamList.getChildren().clear();

        Player active = state.getCurrentPlayer();
        VictoryService victoryService = new VictoryService();
        for (Player player : state.getPlayers()) {
            boolean isActive = player.equals(active);
            int vp = victoryService.calculateVictoryPoints(player);
            addTeamRow(
                    player.getName(),
                    cssColor(player.getColor()),
                    vp,
                    player.getOwnedPipes().size(),
                    ownedPosts(player),
                    ownedLabs(player),
                    player.getTotalResourceCards(),
                    player.getHandCardCount(),
                    isActive
            );
        }

        installResourceBar(active);
    }

    private void installPreviewData() {
        teamList.getChildren().clear();
        addTeamRow("Stewart", "red", 2, 1, 2, 0, 3, 0, true);
        addTeamRow("Gro", "blue", 4, 4, 2, 1, 5, 2, false);
        addTeamRow("Kebin", "gold", 3, 2, 2, 0, 2, 1, false);
        installPreviewResources();
        phaseLabel.setText("Preview Board");
        timerValue.setText("--:--");
        buildPostBtn.setDisable(true);
        buildPipeBtn.setDisable(true);
        upgradeLabBtn.setDisable(true);
        resolveNimonBtn.setDisable(true);
        rollDiceBtn.setDisable(true);
        endTurnBtn.setDisable(true);
    }

    private void installResourceBar(Player active) {
        resBar.getChildren().clear();
        int vp = new VictoryService().calculateVictoryPoints(active);
        StackPane vpNode = new StackPane(new Label(String.valueOf(vp)));
        vpNode.getStyleClass().add("res-chip-vp");
        resBar.getChildren().add(vpNode);

        resBar.getChildren().add(resourceChip(ResourceIcons.Kind.WOOD, active.getResourceAmount(ResourceType.WOOD)));
        resBar.getChildren().add(resourceChip(ResourceIcons.Kind.BRICK, active.getResourceAmount(ResourceType.BRICK)));
        resBar.getChildren().add(resourceChip(ResourceIcons.Kind.WHEAT, active.getResourceAmount(ResourceType.WHEAT)));
        resBar.getChildren().add(resourceChip(ResourceIcons.Kind.ORE, active.getResourceAmount(ResourceType.ORE)));
        resBar.getChildren().add(resourceChip(ResourceIcons.Kind.BANANA, active.getResourceAmount(ResourceType.BANANA)));
    }

    private void installPreviewResources() {
        resBar.getChildren().clear();
        StackPane vpNode = new StackPane(new Label("2"));
        vpNode.getStyleClass().add("res-chip-vp");
        resBar.getChildren().add(vpNode);
        resBar.getChildren().add(resourceChip(ResourceIcons.Kind.WOOD, 2));
        resBar.getChildren().add(resourceChip(ResourceIcons.Kind.BRICK, 1));
        resBar.getChildren().add(resourceChip(ResourceIcons.Kind.WHEAT, 0));
        resBar.getChildren().add(resourceChip(ResourceIcons.Kind.ORE, 1));
        resBar.getChildren().add(resourceChip(ResourceIcons.Kind.BANANA, 2));
        updateDiceDisplay(null, "Roll the dice");
    }

    private void installSidebarHoverCards() {
        bindSidebarHover(scoreboardBtn, () -> createInfoCard("Scoreboard"));
        bindSidebarHover(tradeBtn, () -> createInfoCard("Trade"));
        bindSidebarHover(cardsBtn, () -> createInfoCard("Cards"));
        bindSidebarHover(buildCostsBtn, () -> createInfoCard("Build Costs"));
        bindSidebarHover(settingsBtn, () -> createInfoCard("Settings"));
        bindSidebarHover(buildPostHintZone, this::buildPostHoverCard);
        bindSidebarHover(buildPipeHintZone, this::buildPipeHoverCard);
        bindSidebarHover(upgradeLabHintZone, this::upgradeLabHoverCard);
        bindSidebarHover(resolveNimonHintZone, this::moveNimonHoverCard);
    }

    private void configureSidebarHoverZones() {
        configureActionHoverZone(buildPostHintZone, buildPostBtn);
        configureActionHoverZone(buildPipeHintZone, buildPipeBtn);
        configureActionHoverZone(upgradeLabHintZone, upgradeLabBtn);
        configureActionHoverZone(resolveNimonHintZone, resolveNimonBtn);
    }

    private void configureActionHoverZone(StackPane hoverZone, Button button) {
        hoverZone.setPickOnBounds(true);
        button.mouseTransparentProperty().bind(button.disabledProperty());
    }

    private void bindSidebarHover(Node node, Supplier<VBox> cardSupplier) {
        node.hoverProperty().addListener((obs, oldHovered, hovered) -> {
            if (hovered) {
                showSidebarHoverCard(node, cardSupplier.get());
            } else if (sidebarHoverAnchor == node) {
                hideSidebarHoverCard();
            }
        });
    }

    private HBox resourceChip(ResourceIcons.Kind kind, int count) {
        HBox chip = new HBox(6);
        chip.getStyleClass().add("res-chip");
        chip.setAlignment(Pos.CENTER_LEFT);
        Group icon = ResourceIcons.of(kind);
        icon.setScaleX(1.5);
        icon.setScaleY(1.5);
        StackPane iconSlot = new StackPane(icon);
        iconSlot.setMinSize(34, 34);
        iconSlot.setPrefSize(34, 34);
        iconSlot.setMaxSize(34, 34);
        Label countLabel = new Label(String.valueOf(count));
        countLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: 900; -fx-text-fill: #f3ead8;");
        chip.getChildren().addAll(iconSlot, countLabel);
        return chip;
    }

    private void addTeamRow(String name, String color, int vp, int pipe, int post, int lab, int cards, int dev, boolean active) {
        VBox row = new VBox(6);
        row.getStyleClass().add("card-dark");
        row.setStyle("-fx-padding: 8 10 10 10;"
                + (active
                ? "-fx-border-color: -p-" + color + "; -fx-border-width: 0 0 0 4;"
                : "-fx-border-width: 0 0 0 4; -fx-border-color: transparent;"));

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        StackPane chip = new StackPane();
        chip.getStyleClass().addAll("initial-chip", "pc-" + color);
        chip.setMinSize(28, 28);
        chip.setMaxSize(28, 28);
        chip.getChildren().add(new Label(String.valueOf(name.charAt(0))));

        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: -ink-on-dark;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        StackPane coin = new StackPane(new Label(String.valueOf(vp)));
        coin.getStyleClass().add("coin");
        coin.setMinSize(26, 26);
        coin.setMaxSize(26, 26);

        header.getChildren().addAll(chip, nameLabel, spacer, coin);

        HBox stats = new HBox(8);
        stats.setAlignment(Pos.CENTER_LEFT);
        stats.getChildren().addAll(
                statCell("PIPE", pipe + "/15"),
                statCell("POST", post + "/5"),
                statCell("LAB", lab + "/4"),
                statCell("CRD", String.valueOf(cards)),
                statCell("DEV", String.valueOf(dev))
        );

        row.getChildren().addAll(header, stats);
        teamList.getChildren().add(row);
    }

    private VBox statCell(String icon, String value) {
        VBox cell = new VBox(2);
        cell.setAlignment(Pos.CENTER);
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 12px;");
        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #f1e6cf;");
        cell.getChildren().addAll(iconLabel, valueLabel);
        return cell;
    }

    private void startUiTimer() {
        uiTimer = new Timeline(new KeyFrame(Duration.millis(250), event -> onUiTick()));
        uiTimer.setCycleCount(Timeline.INDEFINITE);
        uiTimer.play();
    }

    private void onUiTick() {
        if (!GameSession.hasEngine()) {
            return;
        }

        GameState state = GameSession.engine().getState();
        if (trackedPlayerId != null
                && !trackedPlayerId.equals(state.getCurrentPlayer().getId())
                && trackedPhase == TurnPhase.TRADE_BUILD
                && state.getTurnState().getPhase() == TurnPhase.RESOURCE_GATHERING) {
            trackedPlayerId = state.getCurrentPlayer().getId();
            trackedPhase = state.getTurnState().getPhase();
            if (uiTimer != null) {
                uiTimer.stop();
            }
            Navigator.goTo("/fxml/turn_transition.fxml");
            return;
        }

        syncTimerAndPhaseUi();
        trackedPlayerId = state.getCurrentPlayer().getId();
        trackedPhase = state.getTurnState().getPhase();
    }

    private void syncTimerAndPhaseUi() {
        if (!GameSession.hasEngine()) {
            return;
        }

        if (GameSession.isStartingOrderPending()) {
            timerValue.setText("--:--");
            timerChip.getStyleClass().remove("is-urgent");
            updatePhaseUi(GameSession.engine().getState());
            return;
        }

        GameEngine engine = GameSession.engine();
        GameState state = engine.getState();
        if (state.getTurnState().getPhase() == TurnPhase.TRADE_BUILD
                && state.getTurnState().getRemainingSeconds() > 0
                && !engine.getTimerService().isRunning()) {
            engine.startTurnTimer(state.getTurnState().getRemainingSeconds());
        }

        int remaining = state.getTurnState().getRemainingSeconds();
        if (engine.getTimerService().isRunning()) {
            remaining = engine.getTimerService().getRemainingSeconds();
            state.getTurnState().setRemainingSeconds(remaining);
        }
        updateTimerDisplay(remaining);
        updatePhaseUi(state);
    }

    private void updateTimerDisplay(int remainingSeconds) {
        if (!GameSession.hasEngine()) {
            timerValue.setText("--:--");
            timerChip.getStyleClass().remove("is-urgent");
            return;
        }

        if (GameSession.engine().getState().getTurnState().getPhase() != TurnPhase.TRADE_BUILD) {
            timerValue.setText("--:--");
            timerChip.getStyleClass().remove("is-urgent");
            return;
        }

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

    private void updatePhaseUi(GameState state) {
        TurnPhase phase = state.getTurnState().getPhase();
        boolean waitingForSetupPipe = state.getTurnState().isWaitingForSetupPipe();
        String playerId = state.getCurrentPlayer().getId();

        if (GameSession.isStartingOrderPending()) {
            phaseLabel.setText("Starting Order: press the dice button to decide who starts.");
            buildPostBtn.setDisable(true);
            buildPipeBtn.setDisable(true);
            upgradeLabBtn.setDisable(true);
            resolveNimonBtn.setDisable(true);
            rollDiceBtn.setDisable(diceAnimationRunning || diceFlowContext != null);
            rollDiceBtn.setText("START");
            endTurnBtn.setDisable(true);
            return;
        }

        phaseLabel.setText(switch (phase) {
            case SETUP -> waitingForSetupPipe
                    ? "Setup Phase: place the pipe connected to the new post."
                    : "Setup Phase: place a monitoring post.";
            case RESOURCE_GATHERING -> "Resource Gathering: roll dice.";
            case DISCARD -> "A 7 was rolled: resolve mandatory discards.";
            case MOVE_NIMON_UNGU -> "Move Nimon Ungu and resolve optional steal.";
            case TRADE_BUILD -> "Trade / Build Phase";
            case GAME_OVER -> "Game Over";
        });

        buildPostBtn.setDisable(true);
        buildPipeBtn.setDisable(true);
        upgradeLabBtn.setDisable(true);
        resolveNimonBtn.setDisable(true);
        rollDiceBtn.setDisable(diceAnimationRunning || diceFlowContext != null || phase != TurnPhase.RESOURCE_GATHERING);
        rollDiceBtn.setText("ROLL");
        endTurnBtn.setDisable(phase != TurnPhase.TRADE_BUILD);

        if (phase == TurnPhase.SETUP) {
            buildPostBtn.setDisable(waitingForSetupPipe || GameSession.engine().getValidSetupPostIds(playerId).isEmpty());
            buildPipeBtn.setDisable(!waitingForSetupPipe || GameSession.engine().getValidSetupRoadIds(playerId).isEmpty());
            return;
        }

        if (phase == TurnPhase.TRADE_BUILD) {
            buildPostBtn.setDisable(GameSession.engine().getValidMonitoringPostIds(playerId).isEmpty());
            buildPipeBtn.setDisable(GameSession.engine().getValidRoadIds(playerId).isEmpty());
            upgradeLabBtn.setDisable(GameSession.engine().getValidLaboratoryUpgradeIds(playerId).isEmpty());
        }
        if (phase == TurnPhase.MOVE_NIMON_UNGU) {
            resolveNimonBtn.setDisable(GameSession.engine().getValidNimonTargetTileIds().isEmpty());
        }

        if (isBoardSelectionActive()) {
            buildPostBtn.setDisable(true);
            buildPipeBtn.setDisable(true);
            upgradeLabBtn.setDisable(true);
            resolveNimonBtn.setDisable(true);
            rollDiceBtn.setDisable(true);
            endTurnBtn.setDisable(true);
        }
    }

    private VBox buildPostHoverCard() {
        if (GameSession.hasEngine()) {
            GameState state = GameSession.engine().getState();
            if (state.getTurnState().getPhase() == TurnPhase.SETUP) {
                return createHintCard(
                        state.getTurnState().isWaitingForSetupPipe() ? "Setup Post" : "Setup Monitoring Post",
                        state.getTurnState().isWaitingForSetupPipe()
                                ? "Finish the free setup pipe first."
                                : "Free during setup."
                );
            }
        }
        return createBuildCostCard("Build Post", buildCostProvider.getCost(BuildActionType.MONITORING_POST));
    }

    private VBox buildPipeHoverCard() {
        if (GameSession.hasEngine()) {
            GameState state = GameSession.engine().getState();
            if (state.getTurnState().getPhase() == TurnPhase.SETUP) {
                return createHintCard(
                        "Setup Pipe",
                        state.getTurnState().isWaitingForSetupPipe()
                                ? "Free during setup."
                                : "Available after the setup post."
                );
            }
        }
        return createBuildCostCard("Build Pipe", buildCostProvider.getCost(BuildActionType.PIPE));
    }

    private VBox upgradeLabHoverCard() {
        return createBuildCostCard("Upgrade Lab", buildCostProvider.getCost(BuildActionType.LABORATORY));
    }

    private VBox moveNimonHoverCard() {
        return createHintCard("Move Nimon", "No material cost.");
    }

    private VBox createInfoCard(String title) {
        return hoverCardBox(title, null);
    }

    private VBox createHintCard(String title, String body) {
        return hoverCardBox(title, body);
    }

    private VBox createBuildCostCard(String title, ResourceInventory cost) {
        VBox box = hoverCardBox(title, null);
        VBox list = new VBox(6);
        list.getStyleClass().add("sidebar-tooltip__list");
        for (ResourceType type : ResourceType.values()) {
            int amount = cost.getAmount(type);
            if (amount <= 0) {
                continue;
            }
            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("sidebar-tooltip__row");

            Group icon = ResourceIcons.of(resourceIconKind(type));
            icon.setScaleX(1.15);
            icon.setScaleY(1.15);
            StackPane iconSlot = new StackPane(icon);
            iconSlot.getStyleClass().add("sidebar-tooltip__icon-slot");

            Label count = new Label(amount + "x " + prettyResourceName(type));
            count.getStyleClass().add("sidebar-tooltip__value");
            row.getChildren().addAll(iconSlot, count);
            list.getChildren().add(row);
        }
        box.getChildren().add(list);
        return box;
    }

    private VBox hoverCardBox(String title, String body) {
        VBox box = new VBox(4);
        box.getStyleClass().add("sidebar-tooltip__box");
        box.setFillWidth(false);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("sidebar-tooltip__title");
        titleLabel.setWrapText(false);
        box.getChildren().add(titleLabel);

        if (body != null && !body.isBlank()) {
            Label bodyLabel = new Label(body);
            bodyLabel.getStyleClass().add("sidebar-tooltip__body");
            bodyLabel.setWrapText(false);
            box.getChildren().add(bodyLabel);
        }
        return box;
    }

    private void showSidebarHoverCard(Node anchorNode, VBox content) {
        if (content == null) {
            hideSidebarHoverCard();
            return;
        }

        sidebarHoverAnchor = anchorNode;
        sidebarHoverPopup.getChildren().setAll(content);
        sidebarHoverPopup.setVisible(true);
        sidebarHoverPopup.applyCss();
        sidebarHoverPopup.autosize();
        double popupWidth = sidebarHoverPopup.prefWidth(-1);
        double popupHeight = sidebarHoverPopup.prefHeight(-1);
        sidebarHoverPopup.resize(popupWidth, popupHeight);
        sidebarHoverPopup.layout();

        Bounds anchorBounds = hud.sceneToLocal(anchorNode.localToScene(anchorNode.getBoundsInLocal()));
        double x = anchorBounds.getMaxX() + 14;
        double y = anchorBounds.getMinY() + (anchorBounds.getHeight() - popupHeight) / 2.0;

        x = Math.min(x, hud.getWidth() - popupWidth - 24);
        y = Math.max(18, Math.min(y, hud.getHeight() - popupHeight - 18));

        sidebarHoverPopup.relocate(x, y);
        sidebarHoverPopup.toFront();
    }

    private void hideSidebarHoverCard() {
        sidebarHoverAnchor = null;
        sidebarHoverPopup.setVisible(false);
        sidebarHoverPopup.getChildren().clear();
    }

    public boolean beginIntersectionSelection(String prompt, List<String> ids, Consumer<String> onSelect) {
        return beginBoardSelection(BoardSelectionType.INTERSECTION, ids, prompt, onSelect, null);
    }

    public boolean beginPathSelection(String prompt, List<String> ids, Consumer<String> onSelect) {
        return beginBoardSelection(BoardSelectionType.PATH, ids, prompt, onSelect, null);
    }

    public boolean beginPathSelection(String prompt, List<String> ids, Consumer<String> onSelect, Runnable onCancel) {
        return beginBoardSelection(BoardSelectionType.PATH, ids, prompt, onSelect, onCancel);
    }

    public boolean beginTileSelection(String prompt, List<String> ids, Consumer<String> onSelect) {
        return beginBoardSelection(BoardSelectionType.TILE, ids, prompt, onSelect, null);
    }

    public boolean beginTileSelection(String prompt, List<String> ids, Consumer<String> onSelect, Runnable onCancel) {
        return beginBoardSelection(BoardSelectionType.TILE, ids, prompt, onSelect, onCancel);
    }

    private boolean beginBoardSelection(
            BoardSelectionType type,
            List<String> ids,
            String prompt,
            Consumer<String> onSelect,
            Runnable onCancel
    ) {
        if (ids == null || ids.isEmpty()) {
            showInfo("Unavailable", "No valid placement spots available.");
            return false;
        }

        pendingBoardSelection = new PendingBoardSelection(type, List.copyOf(ids), prompt, onSelect, onCancel);
        renderBoardSelection();
        log("[Select] " + prompt + " Right-click on the map to cancel.");
        syncTimerAndPhaseUi();
        return true;
    }

    private boolean isBoardSelectionActive() {
        return pendingBoardSelection != null;
    }

    private void renderBoardSelection() {
        if (boardSelectionLayer == null) {
            return;
        }

        boardSelectionLayer.getChildren().clear();
        if (pendingBoardSelection == null) {
            return;
        }

        Rectangle clickCatcher = new Rectangle(BOARD_DESIGN_W, BOARD_DESIGN_H);
        clickCatcher.setFill(Color.color(0, 0, 0, 0.001));
        clickCatcher.setStroke(null);
        clickCatcher.setOnMousePressed(MouseEvent::consume);
        clickCatcher.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                event.consume();
                cancelBoardSelection();
                return;
            }
            event.consume();
        });
        boardSelectionLayer.getChildren().add(clickCatcher);

        switch (pendingBoardSelection.type()) {
            case INTERSECTION -> pendingBoardSelection.ids().forEach(this::addIntersectionSelectionBubble);
            case PATH -> pendingBoardSelection.ids().forEach(this::addPathSelectionBubble);
            case TILE -> pendingBoardSelection.ids().forEach(this::addTileSelectionBubble);
        }
    }

    private void addIntersectionSelectionBubble(String intersectionId) {
        var point = board.getIntersectionPoint(intersectionId);
        if (point == null) {
            return;
        }

        Circle halo = new Circle(point.getX(), point.getY(), 16);
        halo.setFill(Color.color(0.99, 0.87, 0.48, 0.22));
        halo.setStroke(Color.web("#ffd23d"));
        halo.setStrokeWidth(2.4);
        halo.setStrokeType(StrokeType.OUTSIDE);

        Circle core = new Circle(point.getX(), point.getY(), 7.5);
        core.setFill(Color.web("#fff7da"));
        core.setStroke(Color.web("#0e3a5a"));
        core.setStrokeWidth(2);

        wireSelectionNode(halo, intersectionId);
        wireSelectionNode(core, intersectionId);
        boardSelectionLayer.getChildren().addAll(halo, core);
    }

    private void addPathSelectionBubble(String pathId) {
        Path path = GameSession.engine().getState().getBoard().getPath(pathId);
        if (path == null) {
            return;
        }

        HexBoard.PathSegment segment = board.getPathSegment(path);
        if (segment == null) {
            return;
        }

        Line glow = new Line(segment.start().getX(), segment.start().getY(), segment.end().getX(), segment.end().getY());
        glow.setStroke(Color.color(0.99, 0.87, 0.48, 0.9));
        glow.setStrokeWidth(14);
        glow.setStrokeLineCap(StrokeLineCap.ROUND);
        glow.setOpacity(0.75);

        Line line = new Line(segment.start().getX(), segment.start().getY(), segment.end().getX(), segment.end().getY());
        line.setStroke(Color.web("#fff7da"));
        line.setStrokeWidth(8);
        line.setStrokeLineCap(StrokeLineCap.ROUND);

        var mid = segment.start().midpoint(segment.end());
        Circle bubble = new Circle(mid.getX(), mid.getY(), 11);
        bubble.setFill(Color.web("#fff7da"));
        bubble.setStroke(Color.web("#0e3a5a"));
        bubble.setStrokeWidth(2.2);

        wireSelectionNode(glow, pathId);
        wireSelectionNode(line, pathId);
        wireSelectionNode(bubble, pathId);
        boardSelectionLayer.getChildren().addAll(glow, line, bubble);
    }

    private void addTileSelectionBubble(String tileId) {
        var center = board.getTileCenter(tileId);
        if (center == null) {
            return;
        }

        Polygon hex = new Polygon();
        double radius = board.getHexRadius() - 8;
        for (int corner = 0; corner < 6; corner++) {
            double angle = Math.toRadians(60.0 * corner - 30.0);
            hex.getPoints().addAll(
                    center.getX() + radius * Math.cos(angle),
                    center.getY() + radius * Math.sin(angle)
            );
        }
        hex.setFill(Color.color(0.99, 0.87, 0.48, 0.18));
        hex.setStroke(Color.web("#ffd23d"));
        hex.setStrokeWidth(3);
        hex.setStrokeLineJoin(StrokeLineJoin.ROUND);

        Circle bubble = new Circle(center.getX(), center.getY(), 14);
        bubble.setFill(Color.web("#fff7da"));
        bubble.setStroke(Color.web("#0e3a5a"));
        bubble.setStrokeWidth(2.2);

        wireSelectionNode(hex, tileId);
        wireSelectionNode(bubble, tileId);
        boardSelectionLayer.getChildren().addAll(hex, bubble);
    }

    private void wireSelectionNode(Node node, String selectionId) {
        node.setOnMousePressed(MouseEvent::consume);
        node.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                event.consume();
                cancelBoardSelection();
                return;
            }
            if (event.getButton() == MouseButton.PRIMARY) {
                event.consume();
                resolveBoardSelection(selectionId);
            }
        });
    }

    private void resolveBoardSelection(String selectionId) {
        PendingBoardSelection selection = pendingBoardSelection;
        pendingBoardSelection = null;
        renderBoardSelection();
        syncTimerAndPhaseUi();
        if (selection != null) {
            selection.onSelect().accept(selectionId);
        }
    }

    private void cancelBoardSelection() {
        PendingBoardSelection selection = pendingBoardSelection;
        pendingBoardSelection = null;
        renderBoardSelection();
        syncTimerAndPhaseUi();
        if (selection != null && selection.onCancel() != null) {
            selection.onCancel().run();
        } else if (GameSession.hasEngine()) {
            refresh();
        }
    }

    private ResourceIcons.Kind resourceIconKind(ResourceType type) {
        return switch (type) {
            case WOOD -> ResourceIcons.Kind.WOOD;
            case BRICK -> ResourceIcons.Kind.BRICK;
            case WHEAT -> ResourceIcons.Kind.WHEAT;
            case ORE -> ResourceIcons.Kind.ORE;
            case BANANA -> ResourceIcons.Kind.BANANA;
        };
    }

    private String prettyResourceName(ResourceType type) {
        return switch (type) {
            case WOOD -> "Wood";
            case BRICK -> "Brick";
            case WHEAT -> "Wheat";
            case ORE -> "Ore";
            case BANANA -> "Banana";
        };
    }

    private void fitBoard() {
        double availW = boardHolder.getWidth()
                - boardHolder.getPadding().getLeft() - boardHolder.getPadding().getRight();
        double availH = boardHolder.getHeight()
                - boardHolder.getPadding().getTop() - boardHolder.getPadding().getBottom();
        if (availW <= 0 || availH <= 0) {
            return;
        }

        double scale = Math.min(availW / BOARD_DESIGN_W, availH / BOARD_DESIGN_H);
        scale = Math.max(BOARD_MIN_SCALE, Math.min(scale, BOARD_MAX_SCALE));
        canvasScale.setX(scale);
        canvasScale.setY(scale);
        canvasTranslate.setX((availW - BOARD_DESIGN_W * scale) / 2 + boardHolder.getPadding().getLeft());
        canvasTranslate.setY((availH - BOARD_DESIGN_H * scale) / 2 + boardHolder.getPadding().getTop());
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

    private void onCanvasScroll(ScrollEvent event) {
        if (event.getTouchCount() > 0) {
            return;
        }
        applyZoom(event.getDeltaY() > 0 ? ZOOM_FACTOR : 1.0 / ZOOM_FACTOR, event.getX(), event.getY());
        event.consume();
    }

    private void onCanvasZoom(ZoomEvent event) {
        applyZoom(event.getZoomFactor(), event.getX(), event.getY());
        event.consume();
    }

    private void onCanvasDragStart(MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY || event.getButton() == MouseButton.MIDDLE) {
            dragStartX = event.getSceneX();
            dragStartY = event.getSceneY();
            translateStartX = canvasTranslate.getX();
            translateStartY = canvasTranslate.getY();
            ((Pane) event.getSource()).setCursor(javafx.scene.Cursor.CLOSED_HAND);
        }
    }

    private void onCanvasDragged(MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY || event.getButton() == MouseButton.MIDDLE) {
            canvasTranslate.setX(translateStartX + (event.getSceneX() - dragStartX));
            canvasTranslate.setY(translateStartY + (event.getSceneY() - dragStartY));
        }
    }

    private void installFrame() {
        frameLayer.widthProperty().addListener((obs, oldW, newW) -> rebuildFrame());
        frameLayer.heightProperty().addListener((obs, oldH, newH) -> rebuildFrame());
        rebuildFrame();
    }

    private void rebuildFrame() {
        frameLayer.getChildren().clear();
        double width = frameLayer.getWidth();
        double height = frameLayer.getHeight();
        if (width > 0 && height > 0) {
            frameLayer.getChildren().add(WoodenFrame.build(width, height));
        }
    }

    private void checkVictory() {
        if (!GameSession.hasEngine()) {
            return;
        }
        if (GameSession.engine().getState().isGameOver()) {
            AudioEngine.get().playSfx(AudioEngine.Sfx.ACHIEVEMENT);
            Navigator.showOverlay("/fxml/victory_dialog.fxml");
        }
    }

    private void queueBotAutomationIfNeeded() {
        if (botAutomationQueued
                || !GameSession.hasEngine()
                || GameSession.isStartingOrderPending()
                || diceAnimationRunning
                || diceFlowContext != null) {
            return;
        }

        GameEngine engine = GameSession.engine();
        boolean pendingBotDiscard = engine.getState().getTurnState().getPhase() == TurnPhase.DISCARD
                && engine.getState().getTurnState().getPendingDiscardPlayerIds().stream()
                .anyMatch(engine::isBotPlayer);
        if (!engine.isCurrentPlayerBot() && !pendingBotDiscard) {
            return;
        }

        botAutomationQueued = true;
        Platform.runLater(() -> {
            botAutomationQueued = false;
            runBotAutomation();
        });
    }

    private void runBotAutomation() {
        if (!GameSession.hasEngine() || GameSession.isStartingOrderPending() || diceAnimationRunning || diceFlowContext != null) {
            return;
        }

        GameEngine engine = GameSession.engine();
        int safety = 0;
        while (GameSession.hasEngine() && safety++ < 24) {
            GameState state = engine.getState();
            TurnPhase phase = state.getTurnState().getPhase();

            if (phase == TurnPhase.DISCARD) {
                boolean discarded = autoResolvePendingBotDiscards(engine);
                if (!engine.isCurrentPlayerBot()) {
                    if (discarded) {
                        refresh();
                    }
                    return;
                }
                if (!engine.getState().getTurnState().getPendingDiscardPlayerIds().isEmpty()) {
                    Navigator.showOverlay("/fxml/discard_dialog.fxml");
                    refresh();
                    return;
                }
                continue;
            }

            if (!engine.isCurrentPlayerBot()) {
                break;
            }

            try {
                switch (phase) {
                    case SETUP -> log("[Bot] " + engine.resolveBotSetupStep());
                    case RESOURCE_GATHERING -> {
                        Player bot = state.getCurrentPlayer();
                        DiceRoll roll = engine.rollDice(DiceMode.RANDOM, null);
                        updateDiceDisplay(roll, bot.getName() + " rolled");
                        log("[Bot] " + bot.getName() + " rolled "
                                + roll.getFirst() + " + " + roll.getSecond() + " = " + roll.total() + ".");
                    }
                    case MOVE_NIMON_UNGU -> log("[Bot] " + engine.resolveBotNimonFlow());
                    case TRADE_BUILD -> log("[Bot] " + engine.executeBotTradeBuildAction());
                    case GAME_OVER -> {
                        checkVictory();
                        refresh();
                        return;
                    }
                    default -> {
                        refresh();
                        return;
                    }
                }
            } catch (RuntimeException ex) {
                log("[Bot] " + state.getCurrentPlayer().getName() + " stopped because: " + ex.getMessage());
                refresh();
                return;
            }

            if (engine.getState().isGameOver()) {
                refresh();
                checkVictory();
                return;
            }
        }
        refresh();
    }

    private boolean autoResolvePendingBotDiscards(GameEngine engine) {
        boolean handled = false;
        List<String> pendingIds = new ArrayList<>(engine.getState().getTurnState().getPendingDiscardPlayerIds());
        for (String playerId : pendingIds) {
            if (!engine.isBotPlayer(playerId)) {
                continue;
            }
            Player player = engine.getState().getPlayerById(playerId);
            int required = player.getTotalResourceCards() / 2;
            engine.discardForSevenAutomatically(playerId);
            log("[Bot] " + player.getName() + " auto-discarded " + required + " resources.");
            handled = true;
        }
        return handled;
    }

    private void promptNimonFlow() {
        GameEngine engine = GameSession.engine();
        GameState state = engine.getState();
        if (state.getTurnState().getPhase() != TurnPhase.MOVE_NIMON_UNGU) {
            return;
        }

        if (!beginTileSelection(
                "Click a highlighted tile to move Nimon Ungu.",
                engine.getValidNimonTargetTileIds(),
                tileId -> {
                    try {
                        GameEngine currentEngine = GameSession.engine();
                        currentEngine.moveNimonAfterSeven(tileId);
                        AudioEngine.get().playSfx(AudioEngine.Sfx.NIMON_UNGU);
                        log("[Nimon] Moved to " + tileId + ".");

                        if (currentEngine.getValidStealTargetsAfterSeven().isEmpty()) {
                            currentEngine.finishNimonAfterSevenWithoutSteal();
                            log("[Nimon] No valid steal target. Trade/build phase begins.");
                        } else {
                            Navigator.showOverlay("/fxml/steal_dialog.fxml");
                        }
                        refresh();
                    } catch (RuntimeException ex) {
                        showError("Nimon Flow Failed", ex.getMessage());
                        refresh();
                    }
                }
        )) {
            return;
        }
    }

    private void beginStartingOrderFlow() {
        GameEngine engine = GameSession.engine();
        startingOrderBaseConfig = engine.getActiveConfig();
        if (startingOrderBaseConfig == null) {
            startingOrderBaseConfig = new GameConfig(
                    engine.getState().getPlayers().stream()
                            .map(player -> new PlayerConfig(player.getName(), player.getColor()))
                            .toList(),
                    BoardMode.FIXED,
                    engine.isManualDiceEnabled()
            );
        }
        startingOrderOriginalOrder = startingOrderBaseConfig.getPlayerConfigs();
        startingOrderContenders = new ArrayList<>(startingOrderOriginalOrder);
        startingOrderRoundRolls.clear();
        startingOrderRollIndex = 0;
        requestNextStartingOrderRoll();
    }

    private void requestNextStartingOrderRoll() {
        if (startingOrderRollIndex >= startingOrderContenders.size()) {
            finalizeStartingOrderRound();
            return;
        }

        PlayerConfig contender = startingOrderContenders.get(startingOrderRollIndex);
        diceFlowContext = DiceFlowContext.STARTING_ORDER;
        showDiceDialog(
                "DETERMINE FIRST PLAYER",
                "Choose how " + contender.getName() + " will roll for starting order.",
                "ROLL FOR " + contender.getName().toUpperCase(),
                GameSession.engine().isManualDiceEnabled()
        );
    }

    private void resolveStartingOrderRoll(DiceDialogResult result) {
        PlayerConfig contender = startingOrderContenders.get(startingOrderRollIndex);
        DiceRoll roll = result.mode() == DiceMode.RANDOM ? randomRoll() : result.manualRoll();
        animateDiceRoll(roll, contender.getName() + " rolled", () -> {
            log("[Start Roll] " + contender.getName() + " rolled "
                    + roll.getFirst() + " + " + roll.getSecond() + " = " + roll.total() + ".");
            startingOrderRoundRolls.put(contender, roll);
            startingOrderRollIndex++;
            requestNextStartingOrderRoll();
        });
    }

    private void finalizeStartingOrderRound() {
        int bestTotal = startingOrderRoundRolls.values().stream()
                .mapToInt(DiceRoll::total)
                .max()
                .orElse(Integer.MIN_VALUE);
        List<PlayerConfig> highest = startingOrderRoundRolls.entrySet().stream()
                .filter(entry -> entry.getValue().total() == bestTotal)
                .map(Map.Entry::getKey)
                .toList();

        if (highest.size() == 1) {
            PlayerConfig starter = highest.getFirst();
            GameEngine rotatedEngine = new GameEngine();
            rotatedEngine.startNewGame(startingOrderBaseConfig.withPlayerConfigs(
                    rotateFromStarter(startingOrderOriginalOrder, starter)
            ));
            GameSession.setEngine(rotatedEngine);
            GameSession.setStartingOrderPending(false);
            DiceRoll starterRoll = startingOrderRoundRolls.get(starter);
            updateDiceDisplay(starterRoll, starter.getName() + " starts first");
            log("[Start Roll] " + starter.getName() + " starts first with "
                    + starterRoll.getFirst() + " + " + starterRoll.getSecond()
                    + " = " + starterRoll.total() + ".");
            resetStartingOrderFlow();
            refresh();
            return;
        }

        String tiedNames = highest.stream()
                .map(PlayerConfig::getName)
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
        log("[Start Roll] Tie at " + bestTotal + " between " + tiedNames + ". Rerolling tied players.");
        updateDiceDisplay(null, "Tie at " + bestTotal + ". Rerolling " + tiedNames + ".");
        startingOrderContenders = new ArrayList<>(highest);
        startingOrderRoundRolls.clear();
        startingOrderRollIndex = 0;
        requestNextStartingOrderRoll();
    }

    private void resolveNormalTurnRoll(DiceDialogResult result) {
        GameEngine engine = GameSession.engine();
        GameState state = engine.getState();
        String playerName = state.getCurrentPlayer().getName();

        try {
                DiceRoll roll = engine.rollDice(result.mode(), result.manualRoll());
            animateDiceRoll(roll, playerName + " rolled", () -> {
                log("[Roll] " + playerName + " rolled "
                        + roll.getFirst() + " + " + roll.getSecond()
                        + " = " + roll.total() + ".");

                if (roll.total() == 7) {
                    if (engine.getState().getTurnState().getPhase() == TurnPhase.DISCARD) {
                        Navigator.showOverlay("/fxml/discard_dialog.fxml");
                    } else if (engine.getState().getTurnState().getPhase() == TurnPhase.MOVE_NIMON_UNGU) {
                        promptNimonFlow();
                    }
                }

                checkVictory();
                refresh();
            });
        } catch (RuntimeException ex) {
            showError("Roll Failed", ex.getMessage());
            refresh();
        }
    }

    private void showDiceDialog(String title, String header, String confirmLabel, boolean manualEnabled) {
        GameSession.setDiceDialogRequest(new DiceDialogRequest(
                title,
                header,
                confirmLabel,
                manualEnabled
        ));
        Navigator.showOverlay("/fxml/dice_roll_dialog.fxml");
    }

    private void animateDiceRoll(DiceRoll finalRoll, String summary, Runnable afterAnimation) {
        if (diceAnimation != null) {
            diceAnimation.stop();
        }

        diceAnimationRunning = true;
        diceAnimation = new Timeline();
        for (int frame = 0; frame < 10; frame++) {
            diceAnimation.getKeyFrames().add(new KeyFrame(Duration.millis(frame * 65L), event -> {
                renderHudDie(dieOneValue, 1 + uiRandom.nextInt(6));
                renderHudDie(dieTwoValue, 1 + uiRandom.nextInt(6));
            }));
        }
        diceAnimation.getKeyFrames().add(new KeyFrame(Duration.millis(680), event -> updateDiceDisplay(finalRoll, summary)));
        diceAnimation.setOnFinished(event -> {
            diceAnimationRunning = false;
            syncTimerAndPhaseUi();
            if (afterAnimation != null) {
                afterAnimation.run();
            }
        });
        diceAnimation.playFromStart();
        syncTimerAndPhaseUi();
    }

    private void resetStartingOrderFlow() {
        startingOrderBaseConfig = null;
        startingOrderOriginalOrder = List.of();
        startingOrderContenders = List.of();
        startingOrderRoundRolls.clear();
        startingOrderRollIndex = 0;
    }

    private DiceRoll randomRoll() {
        return DiceRoll.of(1 + (int) (Math.random() * 6), 1 + (int) (Math.random() * 6));
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        AudioEngine.get().playSfx(AudioEngine.Sfx.ERROR);
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void updateDiceDisplay(DiceRoll roll, String summary) {
        if (roll == null) {
            renderHudDie(dieOneValue, 1);
            renderHudDie(dieTwoValue, 1);
            return;
        }
        renderHudDie(dieOneValue, roll.getFirst());
        renderHudDie(dieTwoValue, roll.getSecond());
    }

    private void renderHudDie(Pane target, int value) {
        DicePips.render(target, value, 70);
    }

    private List<PlayerConfig> rotateFromStarter(List<PlayerConfig> configs, PlayerConfig starter) {
        int startIndex = configs.indexOf(starter);
        List<PlayerConfig> rotated = new ArrayList<>(configs.size());
        for (int i = 0; i < configs.size(); i++) {
            rotated.add(configs.get((startIndex + i) % configs.size()));
        }
        return rotated;
    }

    private static int ownedPosts(Player player) {
        return (int) player.getOwnedBuildings().stream()
                .filter(building -> building.getType() == com.bananarepublic.model.building.BuildingType.MONITORING_POST)
                .count();
    }

    private static int ownedLabs(Player player) {
        return (int) player.getOwnedBuildings().stream()
                .filter(building -> building.getType() == com.bananarepublic.model.building.BuildingType.LABORATORY)
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

    private enum DiceFlowContext {
        NORMAL_TURN,
        STARTING_ORDER
    }
}
