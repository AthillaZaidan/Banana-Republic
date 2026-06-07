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
import com.bananarepublic.ui.GameIcons;
import com.bananarepublic.ui.GameSession;
import com.bananarepublic.ui.HexBoard;
import com.bananarepublic.ui.LivingBackground;
import com.bananarepublic.ui.Navigator;
import com.bananarepublic.ui.ResourceIcons;
import com.bananarepublic.ui.WoodenFrame;
import javafx.application.Platform;
import javafx.animation.Animation;
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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.StringJoiner;
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
    @FXML private VBox dicePanel;
    @FXML private Button endTurnBtn;

    private Timeline uiTimer;

    private static final double BOARD_DESIGN_W = 900;
    private static final double BOARD_DESIGN_H = 780;
    private static final double BG_MULT = 4.0;
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
        // livingLayer kept for API compat but game-screen world elements go into boardCanvas below
        livingLayer.setMouseTransparent(true);
        AudioEngine.get().playGameBgm();
        dicePanel.setCursor(javafx.scene.Cursor.HAND);
        dicePanel.setPickOnBounds(true);
        dicePanel.setOnMouseReleased(event -> {
            if (!event.isStillSincePress() || event.getButton() != javafx.scene.input.MouseButton.PRIMARY) {
                return;
            }
            event.consume();
            onRollDice();
        });
        configureSidebarHoverZones();
        installSidebarHoverCards();

        board = new HexBoard(GameSession.hasEngine() ? GameSession.engine().getState() : null, BOARD_DESIGN_W, BOARD_DESIGN_H);
        boardSelectionLayer = new Pane();
        boardSelectionLayer.setPickOnBounds(false);
        boardSelectionLayer.setMouseTransparent(false);
        boardSelectionLayer.setPrefSize(BOARD_DESIGN_W, BOARD_DESIGN_H);

        // OCEAN.png fills a large area centered on the board so it never shows edges during zoom/pan
        double bgW = BOARD_DESIGN_W * 4.0;
        double bgH = BOARD_DESIGN_H * 4.0;
        ImageView oceanBg = new ImageView();
        try (var stream = getClass().getResourceAsStream("/images/background/OCEAN.png")) {
            if (stream != null) {
                oceanBg.setImage(new Image(stream));
            }
        } catch (Exception ignored) {}
        oceanBg.setFitWidth(bgW);
        oceanBg.setFitHeight(bgH);
        oceanBg.setPreserveRatio(false);
        oceanBg.setX(-BOARD_DESIGN_W * 1.5);
        oceanBg.setY(-BOARD_DESIGN_H * 1.5);
        oceanBg.setMouseTransparent(true);

        boardCanvas = new Group(oceanBg, board, boardSelectionLayer);
        boardCanvas.getTransforms().addAll(canvasTranslate, canvasScale);

        // Attach animated world-space elements (clouds, gulls, ships) into boardCanvas
        LivingBackground.attachToCanvas(boardCanvas, BOARD_DESIGN_W, BOARD_DESIGN_H);

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
        canvasPane.setOnMouseReleased(this::onCanvasDragEnd);
        canvasPane.setCursor(javafx.scene.Cursor.DEFAULT);

        if (GameSession.hasEngine()) {
            GameState state = GameSession.engine().getState();
            installFromEngine(state);
            trackedPlayerId = state.getCurrentPlayer().getId();
            trackedPhase = state.getTurnState().getPhase();
            updateDiceDisplay(null, GameSession.isStartingOrderPending() ? "START" : "ROLL DICE");
        } else {
            installPreviewData();
        }

        renderPersistedLogbook();
        if (GameSession.hasEngine() && !GameSession.hasLogEntries()) {
            GameState state = GameSession.engine().getState();
            if (GameSession.isStartingOrderPending()) {
                updateDiceDisplay(null, "START");
                log("[Setup] Determine the first player from inside the map.");
            } else {
                log("[Turn] " + state.getCurrentPlayer().getName() + " starts the turn.");
            }
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
        GameSession.appendLogEntry(entry);
        appendLogEntryToUi(entry);
    }

    private void renderPersistedLogbook() {
        logbook.getChildren().clear();
        for (String entry : GameSession.getLogEntries()) {
            appendLogEntryToUi(entry);
        }
    }

    private void appendLogEntryToUi(String entry) {
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
        if (GameSession.hasEngine()) {
            TurnPhase phase = GameSession.engine().getState().getTurnState().getPhase();
            if (phase != TurnPhase.RESOURCE_GATHERING && phase != TurnPhase.TRADE_BUILD) {
                showInfo("Unavailable", "Cards cannot be played during " + phase + ".");
                return;
            }
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
                                Map<ResourceType, Integer> resourcesBefore = snapshotPlayerResources(currentState.getCurrentPlayer());
                                boolean grantsInitialResources = currentState.getTurnState().getSetupRound() == 2;
                                currentEngine.placeSetupWatchPost(playerId, intersectionId);
                                AudioEngine.get().playSfx(AudioEngine.Sfx.BUILD);
                                log("[Setup] " + currentState.getCurrentPlayer().getName()
                                        + " placed a monitoring post at " + intersectionId + ".");
                                if (grantsInitialResources) {
                                    logPositiveResourceDelta(
                                            "[Setup] " + currentState.getCurrentPlayer().getName() + " received initial resources: ",
                                            resourcesBefore,
                                            currentEngine.getState().getCurrentPlayer()
                                    );
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

    private void onRollDice() {
        normalizeStaleDiceUiState();
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
                updateDiceDisplay(null, "START");
                refresh();
                return;
            }
            refresh();
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
        String endedPlayerName = engine.getState().getCurrentPlayer().getName();
        try {
            if (engine.getState().isGameOver()) {
                Navigator.showOverlay("/fxml/victory_dialog.fxml");
                return;
            }
            engine.endTurn();
            log("[Turn] " + endedPlayerName + " ended the turn. "
                    + engine.getState().getCurrentPlayer().getName() + " is up next.");
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
            int vp = displayedVictoryPoints(victoryService, player, active);
            addTeamRow(
                    player.getName(),
                    cssColor(player.getColor()),
                    vp,
                    player.getOwnedPipes().size(),
                    ownedPosts(player),
                    ownedLabs(player),
                    player.getPlayedKnightCount(),
                    player.getTotalResourceCards(),
                    player.getHandCardCount(),
                    isActive
            );
        }

        installResourceBar(active);
    }

    private int displayedVictoryPoints(VictoryService victoryService, Player player, Player activeViewer) {
        if (player.equals(activeViewer)) {
            return victoryService.calculateVictoryPoints(player);
        }
        return victoryService.calculatePublicVictoryPoints(player);
    }

    private void installPreviewData() {
        teamList.getChildren().clear();
        addTeamRow("Stewart", "red", 2, 1, 2, 0, 0, 3, 0, true);
        addTeamRow("Gro", "blue", 4, 4, 2, 1, 1, 5, 2, false);
        addTeamRow("Kebin", "gold", 3, 2, 2, 0, 0, 2, 1, false);
        installPreviewResources();
        phaseLabel.setText("Preview Board");
        timerValue.setText("--:--");
        buildPostBtn.setDisable(true);
        buildPipeBtn.setDisable(true);
        upgradeLabBtn.setDisable(true);
        resolveNimonBtn.setDisable(true);
        dicePanel.setDisable(true);
        endTurnBtn.setDisable(true);
    }

    private VBox vpChip(int vp) {
        VBox wrap = new VBox(4);
        wrap.setAlignment(Pos.CENTER);
        StackPane vpNode = new StackPane(new Label(String.valueOf(vp)));
        vpNode.getStyleClass().add("res-chip-vp");
        Label lbl = new Label("VP");
        lbl.getStyleClass().add("bottom-btn-label");
        wrap.getChildren().addAll(vpNode, lbl);
        return wrap;
    }

    private VBox resourceChipWrapped(ResourceIcons.Kind kind, int count, String label) {
        VBox wrap = new VBox(4);
        wrap.setAlignment(Pos.CENTER);
        wrap.getChildren().addAll(resourceChip(kind, count), makeBottomLabel(label));
        return wrap;
    }

    private Label makeBottomLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("bottom-btn-label");
        return l;
    }

    private void installResourceBar(Player active) {
        resBar.getChildren().clear();
        int vp = new VictoryService().calculateVictoryPoints(active);
        resBar.getChildren().add(vpChip(vp));

        resBar.getChildren().add(resourceChipWrapped(ResourceIcons.Kind.WOOD,   active.getResourceAmount(ResourceType.WOOD),   "WOOD"));
        resBar.getChildren().add(resourceChipWrapped(ResourceIcons.Kind.BRICK,  active.getResourceAmount(ResourceType.BRICK),  "BRICK"));
        resBar.getChildren().add(resourceChipWrapped(ResourceIcons.Kind.WHEAT,  active.getResourceAmount(ResourceType.WHEAT),  "WHEAT"));
        resBar.getChildren().add(resourceChipWrapped(ResourceIcons.Kind.ORE,    active.getResourceAmount(ResourceType.ORE),    "ORE"));
        resBar.getChildren().add(resourceChipWrapped(ResourceIcons.Kind.BANANA, active.getResourceAmount(ResourceType.BANANA), "BANANA"));
    }

    private void installPreviewResources() {
        resBar.getChildren().clear();
        resBar.getChildren().add(vpChip(2));
        resBar.getChildren().add(resourceChipWrapped(ResourceIcons.Kind.WOOD,   2, "WOOD"));
        resBar.getChildren().add(resourceChipWrapped(ResourceIcons.Kind.BRICK,  1, "BRICK"));
        resBar.getChildren().add(resourceChipWrapped(ResourceIcons.Kind.WHEAT,  0, "WHEAT"));
        resBar.getChildren().add(resourceChipWrapped(ResourceIcons.Kind.ORE,    1, "ORE"));
        resBar.getChildren().add(resourceChipWrapped(ResourceIcons.Kind.BANANA, 2, "BANANA"));
        updateDiceDisplay(null, "ROLL DICE");
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

    private VBox resourceChip(ResourceIcons.Kind kind, int count) {
        VBox container = new VBox(2);
        container.getStyleClass().add("res-chip");
        container.setAlignment(Pos.CENTER);

        Group icon = ResourceIcons.of(kind);
        icon.setScaleX(1.1);
        icon.setScaleY(1.1);
        StackPane iconSlot = new StackPane(icon);
        iconSlot.setMinSize(26, 26);
        iconSlot.setPrefSize(26, 26);
        iconSlot.setMaxSize(26, 26);
        iconSlot.setAlignment(Pos.CENTER);

        Label countLabel = new Label(String.valueOf(count));
        countLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 900; -fx-text-fill: #f3ead8; -fx-font-family: 'Gemunu Libre';");

        container.getChildren().addAll(iconSlot, countLabel);
        return container;
    }

    private void addTeamRow(String name, String color, int vp, int pipe, int post, int lab, int knights, int cards, int dev, boolean active) {
        VBox row = new VBox(0);
        row.getStyleClass().add("team-row");
        if (active) row.getStyleClass().add("team-row--active");
        row.setStyle(active
                ? "-fx-border-color: -p-" + color + "; -fx-border-width: 0 0 0 3.5;"
                : "-fx-border-color: transparent; -fx-border-width: 0 0 0 3.5;");

        // ── header ──────────────────────────────────────────────
        HBox header = new HBox(8);
        header.getStyleClass().add("team-row__header");
        header.setAlignment(Pos.CENTER_LEFT);

        StackPane chip = new StackPane();
        chip.getStyleClass().addAll("initial-chip", "pc-" + color);
        chip.setMinSize(26, 26);
        chip.setMaxSize(26, 26);
        Label initial = new Label(String.valueOf(name.charAt(0)));
        initial.setStyle("-fx-font-size: 13px; -fx-font-weight: 900; -fx-text-fill: rgba(255,255,255,0.92);");
        chip.getChildren().add(initial);

        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-font-weight: 800; -fx-font-size: 12px; -fx-text-fill: -ink-on-dark;");
        HBox.setHgrow(nameLabel, Priority.ALWAYS);

        Label activeTag = new Label(active ? "YOUR TURN" : "");
        activeTag.setStyle("-fx-font-size: 8px; -fx-font-weight: 900; -fx-text-fill: -gold-1;"
                + "-fx-background-color: rgba(245,183,56,0.14); -fx-background-radius: 4;"
                + "-fx-padding: 1 5;");

        StackPane coin = new StackPane(new Label(String.valueOf(vp)));
        coin.getStyleClass().add("coin");
        coin.setMinSize(24, 24);
        coin.setMaxSize(24, 24);

        header.getChildren().addAll(chip, nameLabel, activeTag, coin);

        // ── divider ─────────────────────────────────────────────
        Region divider = new Region();
        divider.getStyleClass().add("team-row__divider");

        // ── stat grid ───────────────────────────────────────────
        HBox stats = new HBox(4);
        stats.getStyleClass().add("team-row__stats");
        stats.setAlignment(Pos.CENTER_LEFT);
        stats.getChildren().addAll(
                statCell(GameIcons.pipe(), String.valueOf(pipe), "/15"),
                statCell(GameIcons.monitoringPost(), String.valueOf(post), "/5"),
                statCell(GameIcons.laboratory(), String.valueOf(lab), "/4"),
                statCell(GameIcons.knight(), String.valueOf(knights), null),
                statCell(GameIcons.cardStack(), String.valueOf(cards), null),
                statCell(GameIcons.scroll(), String.valueOf(dev), null)
        );

        row.getChildren().addAll(header, divider, stats);
        teamList.getChildren().add(row);
    }

    private VBox statCell(Group iconGroup, String value, String cap) {
        VBox cell = new VBox(2);
        cell.getStyleClass().add("team-stat-cell");
        cell.setAlignment(Pos.CENTER);

        StackPane iconSlot = new StackPane(iconGroup);
        iconSlot.setMinSize(18, 18);
        iconSlot.setMaxSize(18, 18);
        iconGroup.setScaleX(0.72);
        iconGroup.setScaleY(0.72);

        HBox valRow = new HBox(0);
        valRow.setAlignment(Pos.CENTER);
        Label valLabel = new Label(value);
        valLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: 900; -fx-text-fill: #f3ead8;");
        valRow.getChildren().add(valLabel);
        if (cap != null) {
            Label capLabel = new Label(cap);
            capLabel.setStyle("-fx-font-size: 8px; -fx-font-weight: 600; -fx-text-fill: #7fa8be;");
            valRow.getChildren().add(capLabel);
        }

        cell.getChildren().addAll(iconSlot, valRow);
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
            String previousPlayerName = state.getPlayers().stream()
                    .filter(player -> player.getId().equals(trackedPlayerId))
                    .map(Player::getName)
                    .findFirst()
                    .orElse("Previous player");
            log("[Turn] " + previousPlayerName + " ended the turn. "
                    + state.getCurrentPlayer().getName() + " is up next.");
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
        recoverMissedBotRollTrigger(state);
    }

    private void recoverMissedBotRollTrigger(GameState state) {
        if (GameSession.isStartingOrderPending()
                || diceAnimationRunning
                || diceFlowContext != null
                || state.getTurnState().getPhase() != TurnPhase.RESOURCE_GATHERING
                || !GameSession.engine().isCurrentPlayerBot()) {
            return;
        }

        queueBotAutomationIfNeeded();
    }

    private void syncTimerAndPhaseUi() {
        if (!GameSession.hasEngine()) {
            return;
        }

        normalizeStaleDiceUiState();

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

    private void normalizeStaleDiceUiState() {
        if (diceAnimationRunning && (diceAnimation == null || diceAnimation.getStatus() != Animation.Status.RUNNING)) {
            diceAnimationRunning = false;
        }
        if (diceFlowContext != null && GameSession.getDiceDialogRequest() == null && !diceAnimationRunning) {
            diceFlowContext = null;
        }
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
            setDicePrompt("START");
            buildPostBtn.setDisable(true);
            buildPipeBtn.setDisable(true);
            upgradeLabBtn.setDisable(true);
            resolveNimonBtn.setDisable(true);
            dicePanel.setDisable(diceAnimationRunning || diceFlowContext != null);

            endTurnBtn.setDisable(true);
            return;
        }

        if (pendingBoardSelection != null && hasVisibleBoardSelectionTargets()) {
            phaseLabel.setText(pendingBoardSelection.prompt());
        } else {
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
        }

        buildPostBtn.setDisable(true);
        buildPipeBtn.setDisable(true);
        upgradeLabBtn.setDisable(true);
        resolveNimonBtn.setDisable(true);
        dicePanel.setDisable(diceAnimationRunning || diceFlowContext != null || phase != TurnPhase.RESOURCE_GATHERING);
        setDicePrompt(switch (phase) {
            case RESOURCE_GATHERING -> "ROLL DICE";
            case SETUP -> "SETUP";
            case DISCARD -> "DISCARD";
            case MOVE_NIMON_UNGU -> "MOVE NIMON";
            case TRADE_BUILD -> "BUILD";
            case GAME_OVER -> "DONE";
        });

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
            dicePanel.setDisable(true);
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

        List<String> renderableIds = ids.stream()
                .filter(id -> isSelectionRenderable(type, id))
                .toList();
        if (renderableIds.isEmpty()) {
            showInfo("Unavailable", "No valid placement spots available on the current board view.");
            return false;
        }

        pendingBoardSelection = new PendingBoardSelection(type, List.copyOf(renderableIds), prompt, onSelect, onCancel);
        renderBoardSelection();
        if (!hasVisibleBoardSelectionTargets()) {
            pendingBoardSelection = null;
            renderBoardSelection();
            showInfo("Unavailable", "Could not show placement markers on the board. Try the action again.");
            return false;
        }
        log("[Select] " + prompt + " Right-click on the map to cancel.");
        syncTimerAndPhaseUi();
        return true;
    }

    private boolean isBoardSelectionActive() {
        if (pendingBoardSelection == null) {
            return false;
        }
        if (!hasVisibleBoardSelectionTargets()) {
            log("[Select] Cleared a stale map placement state.");
            pendingBoardSelection = null;
            renderBoardSelection();
            syncTimerAndPhaseUi();
            return false;
        }
        return true;
    }

    private boolean isSelectionRenderable(BoardSelectionType type, String id) {
        if (!GameSession.hasEngine() || id == null || id.isBlank()) {
            return false;
        }
        return switch (type) {
            case INTERSECTION -> board.getIntersectionPoint(id) != null;
            case TILE -> board.getTileCenter(id) != null;
            case PATH -> {
                Path path = GameSession.engine().getState().getBoard().getPath(id);
                yield path != null && board.getPathSegment(path) != null;
            }
        };
    }

    private boolean hasVisibleBoardSelectionTargets() {
        return boardSelectionLayer != null && boardSelectionLayer.getChildren().size() > 1;
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
        clickCatcher.setOnMouseDragged(MouseEvent::consume);
        clickCatcher.setOnMouseReleased(event -> {
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
        node.setOnMouseDragged(MouseEvent::consume);
        node.setOnMouseReleased(event -> {
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

    private String logResourceName(ResourceType type) {
        return switch (type) {
            case WOOD -> "Kayu";
            case BRICK -> "Batu Bata";
            case WHEAT -> "Gandum";
            case ORE -> "Bijih";
            case BANANA -> "Pisang";
        };
    }

    private Map<String, Map<ResourceType, Integer>> snapshotAllPlayerResources(GameState state) {
        Map<String, Map<ResourceType, Integer>> snapshot = new LinkedHashMap<>();
        for (Player player : state.getPlayers()) {
            snapshot.put(player.getId(), snapshotPlayerResources(player));
        }
        return snapshot;
    }

    private Map<ResourceType, Integer> snapshotPlayerResources(Player player) {
        Map<ResourceType, Integer> snapshot = new EnumMap<>(ResourceType.class);
        for (ResourceType type : ResourceType.values()) {
            snapshot.put(type, player.getResourceAmount(type));
        }
        return snapshot;
    }

    private void logRollResourceDistribution(int diceTotal, Map<String, Map<ResourceType, Integer>> before, GameState after) {
        boolean loggedGain = false;
        for (Player player : after.getPlayers()) {
            String delta = describePositiveDelta(before.get(player.getId()), player);
            if (delta == null) {
                continue;
            }
            loggedGain = true;
            log("[Resource] " + player.getName() + " mendapatkan " + delta + " dari roll " + diceTotal + ".");
        }
        if (!loggedGain) {
            log("[Resource] Roll " + diceTotal + " tidak menghasilkan material untuk pemain mana pun.");
        }
    }

    private void logPositiveResourceDelta(String prefix, Map<ResourceType, Integer> before, Player player) {
        String delta = describePositiveDelta(before, player);
        if (delta != null) {
            log(prefix + delta + ".");
        }
    }

    private String describePositiveDelta(Map<ResourceType, Integer> before, Player player) {
        if (before == null) {
            return null;
        }
        StringJoiner joiner = new StringJoiner(", ");
        for (ResourceType type : ResourceType.values()) {
            int previous = before.getOrDefault(type, 0);
            int gained = player.getResourceAmount(type) - previous;
            if (gained > 0) {
                joiner.add(gained + " " + logResourceName(type));
            }
        }
        String result = joiner.toString();
        return result.isBlank() ? null : result;
    }

    private void logSevenResolution(GameEngine engine) {
        List<String> pendingNames = engine.getState().getTurnState().getPendingDiscardPlayerIds().stream()
                .map(playerId -> engine.getState().getPlayerById(playerId).getName())
                .sorted()
                .toList();
        if (pendingNames.isEmpty()) {
            log("[Nimon] Roll 7 activated Nimon Ungu. No discard is required.");
            return;
        }
        log("[Nimon] Roll 7 activated Nimon Ungu. Discard required for: " + String.join(", ", pendingNames) + ".");
    }

    private double computeMinScale(double availW, double availH) {
        double minByW = availW / (BG_MULT * BOARD_DESIGN_W);
        double minByH = availH / (BG_MULT * BOARD_DESIGN_H);
        return Math.max(minByW, minByH);
    }

    private void fitBoard() {
        double availW = boardHolder.getWidth()
                - boardHolder.getPadding().getLeft() - boardHolder.getPadding().getRight();
        double availH = boardHolder.getHeight()
                - boardHolder.getPadding().getTop() - boardHolder.getPadding().getBottom();
        if (availW <= 0 || availH <= 0) {
            return;
        }

        double minScale = computeMinScale(availW, availH);
        double scale = Math.min(availW / BOARD_DESIGN_W, availH / BOARD_DESIGN_H);
        scale = Math.max(minScale, Math.min(scale, BOARD_MAX_SCALE));
        canvasScale.setX(scale);
        canvasScale.setY(scale);
        canvasTranslate.setX((availW - BOARD_DESIGN_W * scale) / 2 + boardHolder.getPadding().getLeft());
        canvasTranslate.setY((availH - BOARD_DESIGN_H * scale) / 2 + boardHolder.getPadding().getTop());
    }

    private void applyZoom(double factor, double pivotX, double pivotY) {
        double availW = boardHolder.getWidth();
        double availH = boardHolder.getHeight();
        double minScale = availW > 0 && availH > 0 ? computeMinScale(availW, availH) : 0.1;

        double oldScale = canvasScale.getX();
        double newScale = Math.max(minScale, Math.min(oldScale * factor, BOARD_MAX_SCALE));
        double ratio = newScale / oldScale;
        double tx = pivotX - ratio * (pivotX - canvasTranslate.getX());
        double ty = pivotY - ratio * (pivotY - canvasTranslate.getY());
        double[] clamped = clampTranslate(tx, ty, newScale);
        canvasScale.setX(newScale);
        canvasScale.setY(newScale);
        canvasTranslate.setX(clamped[0]);
        canvasTranslate.setY(clamped[1]);
    }

    private void onCanvasScroll(ScrollEvent event) {
        if (isBoardSelectionActive()) {
            event.consume();
            return;
        }
        if (event.getTouchCount() > 0) {
            return;
        }
        applyZoom(event.getDeltaY() > 0 ? ZOOM_FACTOR : 1.0 / ZOOM_FACTOR, event.getX(), event.getY());
        event.consume();
    }

    private void onCanvasZoom(ZoomEvent event) {
        if (isBoardSelectionActive()) {
            event.consume();
            return;
        }
        applyZoom(event.getZoomFactor(), event.getX(), event.getY());
        event.consume();
    }

    private void onCanvasDragStart(MouseEvent event) {
        if (isBoardSelectionActive() && event.getButton() == MouseButton.PRIMARY) {
            event.consume();
            return;
        }
        if (event.getButton() == MouseButton.PRIMARY || event.getButton() == MouseButton.MIDDLE) {
            dragStartX = event.getSceneX();
            dragStartY = event.getSceneY();
            translateStartX = canvasTranslate.getX();
            translateStartY = canvasTranslate.getY();
            ((Pane) event.getSource()).setCursor(javafx.scene.Cursor.CLOSED_HAND);
        }
    }

    private void onCanvasDragged(MouseEvent event) {
        if (isBoardSelectionActive() && event.getButton() == MouseButton.PRIMARY) {
            event.consume();
            return;
        }
        if (event.getButton() == MouseButton.PRIMARY || event.getButton() == MouseButton.MIDDLE) {
            double tx = translateStartX + (event.getSceneX() - dragStartX);
            double ty = translateStartY + (event.getSceneY() - dragStartY);
            double[] clamped = clampTranslate(tx, ty, canvasScale.getX());
            canvasTranslate.setX(clamped[0]);
            canvasTranslate.setY(clamped[1]);
        }
    }

    private void onCanvasDragEnd(MouseEvent event) {
        if (event.getSource() instanceof Pane pane) {
            pane.setCursor(javafx.scene.Cursor.DEFAULT);
        }
    }

    private double[] clampTranslate(double tx, double ty, double scale) {
        double vw = boardHolder.getWidth();
        double vh = boardHolder.getHeight();
        double bgX = -1.5 * BOARD_DESIGN_W;
        double bgY = -1.5 * BOARD_DESIGN_H;
        double bgW = BG_MULT * BOARD_DESIGN_W;
        double bgH = BG_MULT * BOARD_DESIGN_H;
        double minTx = -(bgX + bgW) * scale + vw;
        double maxTx = -bgX * scale;
        double minTy = -(bgY + bgH) * scale + vh;
        double maxTy = -bgY * scale;
        return new double[]{
            Math.max(minTx, Math.min(tx, maxTx)),
            Math.max(minTy, Math.min(ty, maxTy))
        };
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
                        autoRollForBotTurn();
                        refresh();
                        return;
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
                            .map(player -> new PlayerConfig(
                                    player.getName(),
                                    player.getColor(),
                                    engine.isBotPlayer(player.getId())
                            ))
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
        if (contender.isBotControlled()) {
            Platform.runLater(() -> onDiceDialogResolved(new DiceDialogResult(DiceMode.RANDOM, null)));
            return;
        }
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
            updateDiceDisplay(starterRoll, "READY");
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
        updateDiceDisplay(null, "REROLL");
        startingOrderContenders = new ArrayList<>(highest);
        startingOrderRoundRolls.clear();
        startingOrderRollIndex = 0;
        requestNextStartingOrderRoll();
    }

    private void resolveNormalTurnRoll(DiceDialogResult result) {
        GameEngine engine = GameSession.engine();
        GameState state = engine.getState();
        String playerName = state.getCurrentPlayer().getName();
        boolean botTurn = engine.isCurrentPlayerBot();
        Map<String, Map<ResourceType, Integer>> resourcesBefore = snapshotAllPlayerResources(state);

        try {
            DiceRoll roll = engine.rollDice(result.mode(), result.manualRoll());
            animateDiceRoll(roll, playerName + " rolled", () -> {
                log((botTurn ? "[Bot] " : "[Roll] ") + playerName + " rolled "
                        + roll.getFirst() + " + " + roll.getSecond()
                        + " = " + roll.total() + ".");

                if (roll.total() == 7) {
                    logSevenResolution(engine);
                    if (engine.getState().getTurnState().getPhase() == TurnPhase.DISCARD
                            && engine.getState().getTurnState().getPendingDiscardPlayerIds().stream()
                            .anyMatch(playerId -> !engine.isBotPlayer(playerId))) {
                        Navigator.showOverlay("/fxml/discard_dialog.fxml");
                    } else if (!botTurn
                            && engine.getState().getTurnState().getPhase() == TurnPhase.MOVE_NIMON_UNGU) {
                        promptNimonFlow();
                    }
                } else {
                    logRollResourceDistribution(roll.total(), resourcesBefore, engine.getState());
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

    private void autoRollForBotTurn() {
        resolveNormalTurnRoll(new DiceDialogResult(DiceMode.RANDOM, null));
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
            setDicePrompt(summary);
            return;
        }
        renderHudDie(dieOneValue, roll.getFirst());
        renderHudDie(dieTwoValue, roll.getSecond());
        setDicePrompt(summary);
    }

    private void setDicePrompt(String prompt) {
        if (diceSummaryLabel == null) {
            return;
        }
        String normalized = prompt == null || prompt.isBlank() ? "ROLL DICE" : prompt.trim();
        diceSummaryLabel.setText(normalized);
    }

    private void renderHudDie(Pane target, int value) {
        DicePips.render(target, value, 54);
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
