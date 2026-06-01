package com.bananarepublic.engine;

import com.bananarepublic.model.board.Board;
import com.bananarepublic.model.card.DevelopmentCard;
import com.bananarepublic.model.card.DevelopmentDeck;
import com.bananarepublic.model.card.KnightCard;
import com.bananarepublic.model.card.MonopolyCard;
import com.bananarepublic.model.card.RoadBuildingCard;
import com.bananarepublic.model.card.VictoryPointCard;
import com.bananarepublic.model.player.Player;
import com.bananarepublic.model.resource.Bank;
import com.bananarepublic.model.resource.ResourceInventory;
import com.bananarepublic.model.resource.ResourceType;
import com.bananarepublic.persistence.SaveLoadService;
import com.bananarepublic.persistence.SnapshotSaveLoadService;
import com.bananarepublic.plugin.Action;
import com.bananarepublic.plugin.BotPluginLoader;
import com.bananarepublic.plugin.GreedyBotStrategy;
import com.bananarepublic.plugin.MapPluginLoader;
import com.bananarepublic.plugin.PlayerStrategy;
import com.bananarepublic.plugin.PluginLoadException;
import com.bananarepublic.plugin.PluginLoader;
import com.bananarepublic.service.board.StandardBoardFactory;
import com.bananarepublic.service.build.BuildService;
import com.bananarepublic.service.dice.DiceMode;
import com.bananarepublic.service.dice.DiceRoll;
import com.bananarepublic.service.dice.DiceService;
import com.bananarepublic.service.resource.ResourceProductionService;
import com.bananarepublic.service.setup.SetupService;
import com.bananarepublic.service.timer.TurnTimerService;
import com.bananarepublic.service.nimon.NimonService;
import com.bananarepublic.service.trade.MaritimeTradeRequest;
import com.bananarepublic.service.trade.TradeOffer;
import com.bananarepublic.service.trade.TradeResult;
import com.bananarepublic.service.trade.TradeService;
import com.bananarepublic.service.victory.VictoryService;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class GameEngine {
    private final TurnManager turnManager;
    private final DiceService diceService;
    private final BuildService buildService;
    private final ResourceProductionService resourceProductionService;
    private final SetupService setupService;
    private final TurnTimerService timerService;
    private final VictoryService victoryService;
    private final TradeService tradeService;
    private final NimonService nimonService;
    private final SaveLoadService saveLoadService;
    private boolean manualDiceEnabled;
    private GameConfig activeConfig;
    private String activeBotPluginJarPath;
    private final Map<String, PlayerStrategy> botStrategies = new HashMap<>();
    private GameState state;

    public GameEngine() {
        this(new TurnManager(), new DiceService(), new BuildService(),
                new ResourceProductionService(), new SetupService(), new TurnTimerService(), new VictoryService(),
                new TradeService(), new NimonService(), new SnapshotSaveLoadService());
    }

    public GameEngine(
            TurnManager turnManager,
            DiceService diceService,
            BuildService buildService,
            ResourceProductionService resourceProductionService,
            SetupService setupService,
            TurnTimerService timerService,
            VictoryService victoryService,
            TradeService tradeService,
            NimonService nimonService
    ) {
        this(turnManager, diceService, buildService, resourceProductionService, setupService,
                timerService, victoryService, tradeService, nimonService, new SnapshotSaveLoadService());
    }

    public GameEngine(
            TurnManager turnManager,
            DiceService diceService,
            BuildService buildService,
            ResourceProductionService resourceProductionService,
            SetupService setupService,
            TurnTimerService timerService,
            VictoryService victoryService,
            TradeService tradeService,
            NimonService nimonService,
            SaveLoadService saveLoadService
    ) {
        this.turnManager = Objects.requireNonNull(turnManager, "Turn manager cannot be null");
        this.diceService = Objects.requireNonNull(diceService, "Dice service cannot be null");
        this.buildService = Objects.requireNonNull(buildService, "Build service cannot be null");
        this.resourceProductionService = Objects.requireNonNull(resourceProductionService, "Resource production service cannot be null");
        this.setupService = Objects.requireNonNull(setupService, "Setup service cannot be null");
        this.timerService = Objects.requireNonNull(timerService, "Timer service cannot be null");
        this.victoryService = Objects.requireNonNull(victoryService, "Victory service cannot be null");
        this.tradeService = Objects.requireNonNull(tradeService, "Trade service cannot be null");
        this.nimonService = Objects.requireNonNull(nimonService, "Nimon service cannot be null");
        this.saveLoadService = Objects.requireNonNull(saveLoadService, "Save/load service cannot be null");
    }

    public void startNewGame(GameConfig config) {
        Objects.requireNonNull(config, "Game config cannot be null");

        Board board = resolveBoard(config);
        Bank bank = new Bank();
        List<Player> players = createPlayers(config);
        DevelopmentDeck deck = DevelopmentDeck.createDefaultDeck();

        turnManager.startSetup(players);
        state = new GameState(board, players, bank, turnManager.getTurnState());
        state.setDevelopmentDeck(deck);
        configureBotStrategies(config, players);
        manualDiceEnabled = config.isManualDiceEnabled();
        activeConfig = config;
        activeBotPluginJarPath = config.getBotPluginJarPath();
    }

    public DiceRoll rollDice(DiceMode mode, DiceRoll manualRoll) {
        requireStarted();
        DiceRoll roll = diceService.roll(mode, manualRoll);
        turnManager.markDiceRolled();

        if (roll.total() == 7) {
            List<String> discardPlayerIds = nimonService.getPlayersWhoMustDiscard(state).stream()
                    .map(Player::getId)
                    .toList();
            state.getTurnState().setPendingDiscardPlayerIds(new HashSet<>(discardPlayerIds));
            state.getTurnState().setNimonMovedThisSeven(false);

            if (discardPlayerIds.isEmpty()) {
                turnManager.moveToPhase(TurnPhase.MOVE_NIMON_UNGU);
            } else {
                turnManager.moveToPhase(TurnPhase.DISCARD);
            }
        } else {
            produceResources(roll.total());
            turnManager.moveToPhase(TurnPhase.TRADE_BUILD);
        }

        return roll;
    }

    public void buildRoad(String playerId, String pathId) {
        requireStarted();
        requireActivePlayer(playerId);
        requireTradeBuildPhase();
        buildService.buildPipe(state, playerId, pathId, false);
        updateSpecialCards();
        updateWinner();
    }

    public void buildRoad(String playerId, String pathId, boolean setupBuild) {
        requireStarted();
        buildService.buildPipe(state, playerId, pathId, setupBuild);
        updateSpecialCards();
        updateWinner();
    }

    public void buildWatchPost(String playerId, String intersectionId) {
        requireStarted();
        requireActivePlayer(playerId);
        requireTradeBuildPhase();
        buildService.buildMonitoringPost(state, playerId, intersectionId, false);
        updateSpecialCards();
        updateWinner();
    }

    public void buildWatchPost(String playerId, String intersectionId, boolean setupBuild) {
        requireStarted();
        buildService.buildMonitoringPost(state, playerId, intersectionId, setupBuild);
        updateSpecialCards();
        updateWinner();
    }

    public void upgradeLaboratory(String playerId, String intersectionId) {
        requireStarted();
        requireActivePlayer(playerId);
        requireTradeBuildPhase();
        buildService.upgradeLaboratory(state, playerId, intersectionId);
        updateSpecialCards();
        updateWinner();
    }

    public void buyDevelopmentCard(String playerId) {
        requireStarted();
        requireActivePlayer(playerId);
        requireTradeBuildPhase();

        DevelopmentDeck deck = state.getDevelopmentDeck();
        if (deck == null || deck.isEmpty()) {
            throw new IllegalStateException("Development deck is empty");
        }

        ResourceInventory cost = new ResourceInventory();
        cost.add(ResourceType.ORE, 1);
        cost.add(ResourceType.BANANA, 1);
        cost.add(ResourceType.WHEAT, 1);

        Player player = state.getCurrentPlayer();
        if (!player.hasResources(cost)) {
            throw new IllegalStateException("Not enough resources to buy development card");
        }

        player.removeResources(cost);
        state.getBank().returnResources(cost);

        DevelopmentCard drawn = deck.draw();
        player.addCard(drawn);
        state.getTurnState().addNewlyBoughtCard(drawn.getId());

        if (drawn instanceof VictoryPointCard) {
            int pointsBefore = victoryService.calculateVictoryPoints(player);
            drawn.play(state, player);
            int pointsAfter = victoryService.calculateVictoryPoints(player);

            if (pointsAfter >= VictoryService.WINNING_POINTS) {
                updateWinner();
            }
        }
    }

    public void playDevelopmentCard(String playerId, String cardId) {
        requireStarted();
        requireActivePlayer(playerId);
        requirePlayablePhase();

        DevelopmentCard card = findAndValidateCard(playerId, cardId);
        if (card instanceof VictoryPointCard) {
            throw new IllegalStateException("Victory point cards stay hidden until the game ends.");
        }
        validateCanPlayCard(cardId);

        card.play(state, state.getCurrentPlayer());
        state.getTurnState().setHasPlayedDevelopmentCard(true);

        removeCardFromHand(playerId, cardId);
        state.getDevelopmentDeck().discard(card);
        updateSpecialCards();
        updateWinner();
    }

    public void playDevelopmentCard(String playerId, String cardId, ResourceType targetResource) {
        requireStarted();
        requireActivePlayer(playerId);
        requirePlayablePhase();
        validateCanPlayCard(cardId);

        DevelopmentCard card = findAndValidateCard(playerId, cardId);
        if (!(card instanceof MonopolyCard monopolyCard)) {
            throw new IllegalArgumentException("This overload is only for Monopoly cards");
        }

        monopolyCard.setTargetResource(targetResource);
        monopolyCard.play(state, state.getCurrentPlayer());
        state.getTurnState().setHasPlayedDevelopmentCard(true);

        removeCardFromHand(playerId, cardId);
        state.getDevelopmentDeck().discard(card);
        updateSpecialCards();
        updateWinner();
    }

    public void playDevelopmentCard(String playerId, String cardId, List<String> pathIds) {
        requireStarted();
        requireActivePlayer(playerId);
        requirePlayablePhase();
        validateCanPlayCard(cardId);

        DevelopmentCard card = findAndValidateCard(playerId, cardId);
        if (!(card instanceof RoadBuildingCard roadCard)) {
            throw new IllegalArgumentException("This overload is only for Road Building cards");
        }

        roadCard.setTargetPathIds(pathIds);
        roadCard.play(state, state.getCurrentPlayer());
        state.getTurnState().setHasPlayedDevelopmentCard(true);

        removeCardFromHand(playerId, cardId);
        state.getDevelopmentDeck().discard(card);
        updateSpecialCards();
        updateWinner();
    }

    public void playDevelopmentCard(String playerId, String cardId, String tileId, String victimPlayerId) {
        requireStarted();
        requireActivePlayer(playerId);
        requirePlayablePhase();
        validateCanPlayCard(cardId);

        DevelopmentCard card = findAndValidateCard(playerId, cardId);
        if (!(card instanceof KnightCard knightCard)) {
            throw new IllegalArgumentException("This overload is only for Knight cards");
        }

        knightCard.setTargetTileId(tileId);
        knightCard.setVictimPlayerId(victimPlayerId);
        knightCard.play(state, state.getCurrentPlayer());
        state.getTurnState().setHasPlayedDevelopmentCard(true);

        removeCardFromHand(playerId, cardId);
        state.getDevelopmentDeck().discard(card);
        updateSpecialCards();
        updateWinner();
    }

    public void loadPluginCards(File jarFile) {
        requireStarted();
        Objects.requireNonNull(jarFile, "JAR file cannot be null");

        PluginLoader loader = new PluginLoader();
        List<DevelopmentCard> pluginCards = loader.loadFromJar(jarFile);
        state.getDevelopmentDeck().addCards(pluginCards);
    }

    public void endTurn() {
        requireStarted();
        timerService.stop();
        updateWinner();

        if (!state.isGameOver()) {
            tradeService.clearPendingOffer();
            turnManager.endTurn();
        }
    }

    public TradeResult submitDomesticTrade(TradeOffer offer) {
        requireStarted();
        return tradeService.submitDomesticTrade(state, offer);
    }

    public TradeResult acceptDomesticTrade(String responderPlayerId) {
        requireStarted();
        return tradeService.acceptDomesticTrade(state, responderPlayerId);
    }

    public TradeResult rejectDomesticTrade(String responderPlayerId) {
        requireStarted();
        return tradeService.rejectDomesticTrade(state, responderPlayerId);
    }

    public TradeResult counterDomesticTrade(String responderPlayerId, TradeOffer counterOffer) {
        requireStarted();
        return tradeService.counterDomesticTrade(state, responderPlayerId, counterOffer);
    }

    public TradeResult submitMaritimeTrade(MaritimeTradeRequest request) {
        requireStarted();
        return tradeService.submitMaritimeTrade(state, request);
    }

    public TradeOffer getPendingTradeOffer() {
        requireStarted();
        return tradeService.getPendingOffer();
    }

    public int getBestMaritimeRatio(String playerId, ResourceType offeredType) {
        requireStarted();
        return tradeService.resolveBestRatio(state, state.getPlayerById(playerId), offeredType);
    }

    public List<Player> getPlayersWhoMustDiscard() {
        requireStarted();
        return nimonService.getPlayersWhoMustDiscard(state);
    }

    public void discardForSeven(String playerId, ResourceInventory discarded) {
        requireStarted();
        if (state.getTurnState().getPhase() != TurnPhase.DISCARD) {
            throw new IllegalStateException("Discard is only available during DISCARD phase");
        }

        nimonService.discardHalfIfNeeded(state, playerId, discarded);
        state.getTurnState().markDiscardDone(playerId);
        if (state.getTurnState().getPendingDiscardPlayerIds().isEmpty()) {
            turnManager.moveToPhase(TurnPhase.MOVE_NIMON_UNGU);
        }
    }

    public void moveNimonAfterSeven(String tileId) {
        requireStarted();
        if (state.getTurnState().getPhase() != TurnPhase.MOVE_NIMON_UNGU) {
            throw new IllegalStateException("Nimon move is only available during MOVE_NIMON_UNGU phase");
        }
        if (!state.getTurnState().getPendingDiscardPlayerIds().isEmpty()) {
            throw new IllegalStateException("All mandatory discards must complete before moving Nimon");
        }

        nimonService.moveNimon(state, tileId);
        state.getTurnState().setNimonMovedThisSeven(true);
    }

    public void stealAfterSeven(String victimPlayerId) {
        requireStarted();
        if (state.getTurnState().getPhase() != TurnPhase.MOVE_NIMON_UNGU) {
            throw new IllegalStateException("Steal is only available during MOVE_NIMON_UNGU phase");
        }
        if (!state.getTurnState().isNimonMovedThisSeven()) {
            throw new IllegalStateException("Nimon must be moved before stealing");
        }
        nimonService.stealRandomResource(state, state.getCurrentPlayer().getId(), victimPlayerId);
        turnManager.moveToPhase(TurnPhase.TRADE_BUILD);
    }

    public List<Player> getValidStealTargetsAfterSeven() {
        requireStarted();
        return nimonService.getValidStealTargets(state, state.getCurrentPlayer().getId());
    }

    public void finishNimonAfterSevenWithoutSteal() {
        requireStarted();
        if (state.getTurnState().getPhase() != TurnPhase.MOVE_NIMON_UNGU) {
            throw new IllegalStateException("Nimon flow is not active");
        }
        if (!state.getTurnState().isNimonMovedThisSeven()) {
            throw new IllegalStateException("Nimon must be moved before finishing");
        }
        turnManager.moveToPhase(TurnPhase.TRADE_BUILD);
    }

    public List<String> getValidSetupPostIds(String playerId) {
        requireStarted();
        return state.getBoard().getIntersections().stream()
                .map(intersection -> intersection.getId())
                .filter(intersectionId -> buildService.canBuildMonitoringPost(state, playerId, intersectionId, true))
                .toList();
    }

    public List<String> getValidSetupRoadIds(String playerId) {
        requireStarted();
        String setupPostIntersectionId = state.getTurnState().getSetupPostIntersectionId();
        if (setupPostIntersectionId == null) {
            return List.of();
        }
        return state.getBoard().getPaths().stream()
                .map(path -> path.getId())
                .filter(pathId -> setupService.isSetupPipeConnectedToPost(state, pathId, setupPostIntersectionId))
                .filter(pathId -> buildService.canBuildPipe(state, playerId, pathId, true))
                .toList();
    }

    public List<String> getValidRoadIds(String playerId) {
        requireStarted();
        return state.getBoard().getPaths().stream()
                .map(path -> path.getId())
                .filter(pathId -> buildService.canBuildPipe(state, playerId, pathId, false))
                .toList();
    }

    public List<String> getValidMonitoringPostIds(String playerId) {
        requireStarted();
        return state.getBoard().getIntersections().stream()
                .map(intersection -> intersection.getId())
                .filter(intersectionId -> buildService.canBuildMonitoringPost(state, playerId, intersectionId, false))
                .toList();
    }

    public List<String> getValidLaboratoryUpgradeIds(String playerId) {
        requireStarted();
        return state.getBoard().getIntersections().stream()
                .map(intersection -> intersection.getId())
                .filter(intersectionId -> buildService.canUpgradeLaboratory(state, playerId, intersectionId))
                .toList();
    }

    public List<String> getValidNimonTargetTileIds() {
        requireStarted();
        return state.getBoard().getTiles().stream()
                .map(tile -> tile.getId())
                .filter(tileId -> !tileId.equals(state.getNimonTileId()))
                .toList();
    }

    public void placeSetupWatchPost(String playerId, String intersectionId) {
        requireStarted();
        requireSetupPlayer(playerId);

        buildService.buildMonitoringPost(state, playerId, intersectionId, true);
        if (state.getTurnState().getSetupRound() == 2) {
            setupService.grantInitialResources(state, playerId, intersectionId);
        }
        turnManager.markSetupPostPlaced(intersectionId);
    }

    public void placeSetupRoad(String playerId, String pathId) {
        requireStarted();
        requireSetupPlayer(playerId);
        String setupPostIntersectionId = state.getTurnState().getSetupPostIntersectionId();

        if (!setupService.isSetupPipeConnectedToPost(state, pathId, setupPostIntersectionId)) {
            throw new IllegalArgumentException("Setup pipe must connect to the just placed monitoring post");
        }

        buildService.buildPipe(state, playerId, pathId, true);
        updateSpecialCards();
        turnManager.completeSetupPipeAndAdvance();
    }

    public void startTurnTimer(int seconds) {
        requireStarted();

        if (state.getTurnState().getPhase() != TurnPhase.TRADE_BUILD) {
            throw new IllegalStateException("Timer can only start during trade/build phase");
        }

        state.getTurnState().setRemainingSeconds(seconds);
        timerService.start(seconds, () -> {
            if (!state.isGameOver() && state.getTurnState().getPhase() == TurnPhase.TRADE_BUILD) {
                endTurn();
            }
        });
    }

    public TurnTimerService getTimerService() {
        return timerService;
    }

    public boolean isManualDiceEnabled() {
        return manualDiceEnabled;
    }

    public GameConfig getActiveConfig() {
        return activeConfig;
    }

    public java.util.Set<String> getBotPlayerIds() {
        requireStarted();
        return java.util.Set.copyOf(botStrategies.keySet());
    }

    public String getActiveBotPluginJarPath() {
        return activeBotPluginJarPath;
    }

    public boolean isCurrentPlayerBot() {
        requireStarted();
        return isBotPlayer(state.getCurrentPlayer().getId());
    }

    public boolean isBotPlayer(String playerId) {
        requireStarted();
        return botStrategies.containsKey(playerId);
    }

    public String resolveBotSetupStep() {
        requireStarted();
        Player player = state.getCurrentPlayer();
        if (!isBotPlayer(player.getId())) {
            throw new IllegalStateException("Current player is not bot-controlled");
        }
        if (state.getTurnState().getPhase() != TurnPhase.SETUP) {
            throw new IllegalStateException("Bot setup action is only valid during setup");
        }

        if (state.getTurnState().isWaitingForSetupPipe()) {
            String pathId = getValidSetupRoadIds(player.getId()).stream().findFirst()
                    .orElseThrow(() -> new IllegalStateException("No valid setup pipe for bot"));
            placeSetupRoad(player.getId(), pathId);
            return player.getName() + " placed a setup pipe on " + pathId + ".";
        }

        String intersectionId = getValidSetupPostIds(player.getId()).stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No valid setup post for bot"));
        placeSetupWatchPost(player.getId(), intersectionId);
        return player.getName() + " placed a setup monitoring post at " + intersectionId + ".";
    }

    public void discardForSevenAutomatically(String playerId) {
        requireStarted();
        Player player = state.getPlayerById(playerId);
        int required = player.getTotalResourceCards() / 2;
        ResourceInventory discarded = new ResourceInventory();
        if (required == 0) {
            discardForSeven(playerId, discarded);
            return;
        }

        Map<ResourceType, Integer> amounts = new EnumMap<>(ResourceType.class);
        for (ResourceType type : ResourceType.values()) {
            amounts.put(type, player.getResourceAmount(type));
        }

        int remaining = required;
        while (remaining > 0) {
            ResourceType nextType = amounts.entrySet().stream()
                    .filter(entry -> entry.getValue() > 0)
                    .max(Comparator.comparingInt(Map.Entry::getValue))
                    .map(Map.Entry::getKey)
                    .orElseThrow(() -> new IllegalStateException("Bot discard could not find enough resources"));
            discarded.add(nextType, 1);
            amounts.put(nextType, amounts.get(nextType) - 1);
            remaining--;
        }

        discardForSeven(playerId, discarded);
    }

    public String resolveBotNimonFlow() {
        requireStarted();
        Player player = state.getCurrentPlayer();
        if (!isBotPlayer(player.getId())) {
            throw new IllegalStateException("Current player is not bot-controlled");
        }
        if (state.getTurnState().getPhase() != TurnPhase.MOVE_NIMON_UNGU) {
            throw new IllegalStateException("Bot Nimon action is only valid during Nimon flow");
        }

        String tileId = getValidNimonTargetTileIds().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No valid tile for bot Nimon move"));
        moveNimonAfterSeven(tileId);

        List<Player> targets = getValidStealTargetsAfterSeven();
        if (targets.isEmpty()) {
            finishNimonAfterSevenWithoutSteal();
            return player.getName() + " moved Nimon to " + tileId + " and skipped steal.";
        }

        String victimId = targets.getFirst().getId();
        stealAfterSeven(victimId);
        return player.getName() + " moved Nimon to " + tileId + " and stole from " + state.getPlayerById(victimId).getName() + ".";
    }

    public String executeBotTradeBuildAction() {
        requireStarted();
        Player player = state.getCurrentPlayer();
        if (!isBotPlayer(player.getId())) {
            throw new IllegalStateException("Current player is not bot-controlled");
        }
        if (state.getTurnState().getPhase() != TurnPhase.TRADE_BUILD) {
            throw new IllegalStateException("Bot action is only valid during trade/build");
        }

        PlayerStrategy strategy = botStrategies.get(player.getId());
        Action action = strategy != null ? strategy.takeTurn(state) : null;
        if (action == null) {
            endTurn();
            return player.getName() + " ended the turn.";
        }

        try {
            action.execute(this, player.getId());
            return player.getName() + " chose to " + action.describe() + ".";
        } catch (RuntimeException ex) {
            endTurn();
            return player.getName() + " attempted an invalid bot action and ended the turn instead: " + ex.getMessage();
        }
    }

    public boolean canBuyDevelopmentCard(String playerId) {
        return getDevelopmentCardPurchaseBlockReason(playerId) == null;
    }

    public String getDevelopmentCardPurchaseBlockReason(String playerId) {
        requireStarted();
        if (!state.getCurrentPlayer().getId().equals(playerId)) {
            return "Bukan giliran pemain ini.";
        }
        if (state.getTurnState().getPhase() != TurnPhase.TRADE_BUILD) {
            return "Kartu hanya bisa dibeli saat fase Trade / Build.";
        }

        DevelopmentDeck deck = state.getDevelopmentDeck();
        if (deck == null || deck.isEmpty()) {
            return "Deck Kartu Temuan sudah habis.";
        }

        ResourceInventory cost = new ResourceInventory();
        cost.add(ResourceType.ORE, 1);
        cost.add(ResourceType.BANANA, 1);
        cost.add(ResourceType.WHEAT, 1);
        if (!state.getCurrentPlayer().hasResources(cost)) {
            return "Butuh 1 Besi, 1 Pisang, dan 1 Gandum untuk membeli kartu.";
        }
        return null;
    }

    public void produceResources(int diceTotal) {
        requireStarted();
        resourceProductionService.produce(state, diceTotal);
    }

    public void recordKnightPlayed(String playerId) {
        requireStarted();
        state.getPlayerById(playerId).incrementPlayedKnightCount();
        updateSpecialCards();
        updateWinner();
    }

    public void checkVictory() {
        requireStarted();
        updateSpecialCards();
        updateWinner();
    }

    public GameState getState() {
        requireStarted();
        return state;
    }

    public void saveGame(File file) {
        requireStarted();
        Objects.requireNonNull(file, "Save file cannot be null");
        if (saveLoadService instanceof SnapshotSaveLoadService snapshotSaveLoadService) {
            snapshotSaveLoadService.save(this, file.toPath());
        } else {
            saveLoadService.save(state, file.toPath());
        }
    }

    public void loadGame(File file) {
        Objects.requireNonNull(file, "Save file cannot be null");
        timerService.stop();
        if (saveLoadService instanceof SnapshotSaveLoadService snapshotSaveLoadService) {
            var snapshot = snapshotSaveLoadService.loadSnapshot(file.toPath());
            state = snapshot.toGameState();
            manualDiceEnabled = snapshot.manualDiceEnabled();
            activeBotPluginJarPath = snapshot.botPluginJarPath();
            restoreBotStrategiesAfterLoad(snapshot.botPlayerIds(), snapshot.botPluginJarPath());
        } else {
            state = saveLoadService.load(file.toPath());
            manualDiceEnabled = true;
            activeBotPluginJarPath = null;
            botStrategies.clear();
        }
        turnManager.restore(state.getPlayers(), state.getTurnState());
        activeConfig = buildLoadedConfig();
    }

    private DevelopmentCard findAndValidateCard(String playerId, String cardId) {
        Player player = state.getPlayerById(playerId);
        DevelopmentCard card = player.findCard(cardId);
        if (card == null) {
            throw new IllegalArgumentException("Card not found in hand: " + cardId);
        }
        return card;
    }

    private void validateCanPlayCard(String cardId) {
        if (state.getTurnState().hasPlayedDevelopmentCard()) {
            throw new IllegalStateException("Already played a development card this turn");
        }
        if (state.getTurnState().isNewlyBoughtCard(cardId)) {
            throw new IllegalStateException("Cannot play a card bought this turn");
        }
    }

    private void removeCardFromHand(String playerId, String cardId) {
        Player player = state.getPlayerById(playerId);
        DevelopmentCard card = player.findCard(cardId);
        if (card != null) {
            player.removeCard(card);
        }
    }

    private void requireActivePlayer(String playerId) {
        if (!state.getCurrentPlayer().getId().equals(playerId)) {
            throw new IllegalArgumentException("It is not this player's turn");
        }
    }

    private void requirePlayablePhase() {
        TurnPhase phase = state.getTurnState().getPhase();
        if (phase != TurnPhase.RESOURCE_GATHERING && phase != TurnPhase.TRADE_BUILD) {
            throw new IllegalStateException("Cannot play development card in " + phase + " phase");
        }
    }

    private void requireTradeBuildPhase() {
        if (state.getTurnState().getPhase() != TurnPhase.TRADE_BUILD) {
            throw new IllegalStateException("Action is only valid during trade/build phase");
        }
    }

    private List<Player> createPlayers(GameConfig config) {
        List<Player> players = new ArrayList<>();
        int counter = 1;

        for (PlayerConfig playerConfig : config.getPlayerConfigs()) {
            players.add(new Player("P" + counter, playerConfig.getName(), playerConfig.getColor()));
            counter++;
        }

        return players;
    }

    private void configureBotStrategies(GameConfig config, List<Player> players) {
        botStrategies.clear();
        String jarPath = config.getBotPluginJarPath();
        for (int i = 0; i < config.getPlayerConfigs().size(); i++) {
            PlayerConfig playerConfig = config.getPlayerConfigs().get(i);
            if (!playerConfig.isBotControlled()) {
                continue;
            }

            PlayerStrategy strategy = jarPath == null || jarPath.isBlank()
                    ? new GreedyBotStrategy()
                    : new BotPluginLoader().loadFromJar(new File(jarPath));
            botStrategies.put(players.get(i).getId(), strategy);
        }
    }

    private void restoreBotStrategiesAfterLoad(java.util.Set<String> botPlayerIds, String jarPath) {
        botStrategies.clear();
        if (botPlayerIds == null || botPlayerIds.isEmpty()) {
            return;
        }

        for (String playerId : botPlayerIds) {
            if (state.getPlayers().stream().noneMatch(player -> player.getId().equals(playerId))) {
                continue;
            }
            PlayerStrategy strategy = jarPath == null || jarPath.isBlank()
                    ? new GreedyBotStrategy()
                    : new BotPluginLoader().loadFromJar(new File(jarPath));
            botStrategies.put(playerId, strategy);
        }
    }

    private GameConfig buildLoadedConfig() {
        List<PlayerConfig> playerConfigs = state.getPlayers().stream()
                .map(player -> new PlayerConfig(
                        player.getName(),
                        player.getColor(),
                        botStrategies.containsKey(player.getId())
                ))
                .toList();
        return new GameConfig(
                playerConfigs,
                BoardMode.FIXED,
                manualDiceEnabled,
                state.getBoard(),
                null,
                activeBotPluginJarPath
        );
    }

    private Board resolveBoard(GameConfig config) {
        if (config.getBoardOverride() != null) {
            return config.getBoardOverride();
        }

        return switch (config.getBoardMode()) {
            case FIXED -> new StandardBoardFactory().createBoard();
            case PLUGIN -> {
                String jarPath = config.getMapPluginJarPath();
                if (jarPath == null) {
                    throw new IllegalArgumentException("Plugin board mode requires a map plugin JAR");
                }
                yield new MapPluginLoader().loadFromJar(new File(jarPath)).generateBoard();
            }
        };
    }

    private void updateWinner() {
        victoryService.findWinner(state).ifPresent(state::setWinner);
    }

    private void updateSpecialCards() {
        victoryService.updateSpecialCards(state);
    }

    private void requireSetupPlayer(String playerId) {
        if (state.getTurnState().getPhase() != TurnPhase.SETUP) {
            throw new IllegalStateException("Action is only valid during setup");
        }

        if (!state.getCurrentPlayer().getId().equals(playerId)) {
            throw new IllegalArgumentException("It is not this player's setup turn");
        }
    }

    private void requireStarted() {
        if (state == null) {
            throw new IllegalStateException("Game has not started");
        }
    }
}
