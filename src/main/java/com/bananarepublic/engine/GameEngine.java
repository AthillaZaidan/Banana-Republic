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
import com.bananarepublic.service.victory.VictoryService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GameEngine {
    private final TurnManager turnManager;
    private final DiceService diceService;
    private final BuildService buildService;
    private final ResourceProductionService resourceProductionService;
    private final SetupService setupService;
    private final TurnTimerService timerService;
    private final VictoryService victoryService;
    private GameState state;

    public GameEngine() {
        this(new TurnManager(), new DiceService(), new BuildService(),
                new ResourceProductionService(), new SetupService(), new TurnTimerService(), new VictoryService());
    }

    public GameEngine(
            TurnManager turnManager,
            DiceService diceService,
            BuildService buildService,
            ResourceProductionService resourceProductionService,
            SetupService setupService,
            TurnTimerService timerService,
            VictoryService victoryService
    ) {
        this.turnManager = Objects.requireNonNull(turnManager, "Turn manager cannot be null");
        this.diceService = Objects.requireNonNull(diceService, "Dice service cannot be null");
        this.buildService = Objects.requireNonNull(buildService, "Build service cannot be null");
        this.resourceProductionService = Objects.requireNonNull(resourceProductionService, "Resource production service cannot be null");
        this.setupService = Objects.requireNonNull(setupService, "Setup service cannot be null");
        this.timerService = Objects.requireNonNull(timerService, "Timer service cannot be null");
        this.victoryService = Objects.requireNonNull(victoryService, "Victory service cannot be null");
    }

    public void startNewGame(GameConfig config) {
        Objects.requireNonNull(config, "Game config cannot be null");

        if (config.getBoardMode() != BoardMode.FIXED) {
            throw new IllegalArgumentException("Unsupported board mode: " + config.getBoardMode());
        }

        Board board = new StandardBoardFactory().createBoard();
        Bank bank = new Bank();
        List<Player> players = createPlayers(config);
        DevelopmentDeck deck = DevelopmentDeck.createDefaultDeck();

        turnManager.startSetup(players);
        state = new GameState(board, players, bank, turnManager.getTurnState());
        state.setDevelopmentDeck(deck);
    }

    public DiceRoll rollDice(DiceMode mode, DiceRoll manualRoll) {
        requireStarted();
        DiceRoll roll = diceService.roll(mode, manualRoll);
        turnManager.markDiceRolled();

        if (roll.total() == 7) {
            turnManager.moveToPhase(TurnPhase.MOVE_NIMON_UNGU);
        } else {
            produceResources(roll.total());
            turnManager.moveToPhase(TurnPhase.TRADE_BUILD);
        }

        return roll;
    }

    public void buildRoad(String playerId, String pathId) {
        requireStarted();
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
        buildService.upgradeLaboratory(state, playerId, intersectionId);
        updateSpecialCards();
        updateWinner();
    }

    public void buyDevelopmentCard(String playerId) {
        requireStarted();
        requireActivePlayer(playerId);
        requirePlayablePhase();

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
        if (!(card instanceof VictoryPointCard)) {
            validateCanPlayCard(cardId);
        }

        card.play(state, state.getCurrentPlayer());

        if (card instanceof VictoryPointCard) {
            updateWinner();
        } else {
            state.getTurnState().setHasPlayedDevelopmentCard(true);
        }

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
            turnManager.endTurn();
        }
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
        if (phase == TurnPhase.SETUP || phase == TurnPhase.GAME_OVER) {
            throw new IllegalStateException("Cannot play development card in " + phase + " phase");
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
