package com.bananarepublic.engine;

import com.bananarepublic.model.board.Board;
import com.bananarepublic.model.player.Player;
import com.bananarepublic.model.resource.Bank;
import com.bananarepublic.service.board.StandardBoardFactory;
import com.bananarepublic.service.build.BuildService;
import com.bananarepublic.service.dice.DiceMode;
import com.bananarepublic.service.dice.DiceRoll;
import com.bananarepublic.service.dice.DiceService;
import com.bananarepublic.service.resource.ResourceProductionService;
import com.bananarepublic.service.victory.VictoryService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GameEngine {
    private final TurnManager turnManager;
    private final DiceService diceService;
    private final BuildService buildService;
    private final ResourceProductionService resourceProductionService;
    private final VictoryService victoryService;
    private GameState state;

    public GameEngine() {
        this(new TurnManager(), new DiceService(), new BuildService(),
                new ResourceProductionService(), new VictoryService());
    }

    public GameEngine(
            TurnManager turnManager,
            DiceService diceService,
            BuildService buildService,
            ResourceProductionService resourceProductionService,
            VictoryService victoryService
    ) {
        this.turnManager = Objects.requireNonNull(turnManager, "Turn manager cannot be null");
        this.diceService = Objects.requireNonNull(diceService, "Dice service cannot be null");
        this.buildService = Objects.requireNonNull(buildService, "Build service cannot be null");
        this.resourceProductionService = Objects.requireNonNull(resourceProductionService, "Resource production service cannot be null");
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

        turnManager.startNormalTurns(players);
        state = new GameState(board, players, bank, turnManager.getTurnState());
    }

    public DiceRoll rollDice(DiceMode mode, DiceRoll manualRoll) {
        requireStarted();
        DiceRoll roll = diceService.roll(mode, manualRoll);
        turnManager.markDiceRolled();

        if (roll.total() == 7) {
            turnManager.moveToPhase(TurnPhase.MOVE_NIMON_UNGU);
        } else {
            resourceProductionService.produce(state, roll.total());
            turnManager.moveToPhase(TurnPhase.TRADE_BUILD);
        }

        return roll;
    }

    public void buildRoad(String playerId, String pathId) {
        requireStarted();
        buildService.buildPipe(state, playerId, pathId, false);
        updateWinner();
    }

    public void buildRoad(String playerId, String pathId, boolean setupBuild) {
        requireStarted();
        buildService.buildPipe(state, playerId, pathId, setupBuild);
        updateWinner();
    }

    public void buildWatchPost(String playerId, String intersectionId) {
        requireStarted();
        buildService.buildMonitoringPost(state, playerId, intersectionId, false);
        updateWinner();
    }

    public void buildWatchPost(String playerId, String intersectionId, boolean setupBuild) {
        requireStarted();
        buildService.buildMonitoringPost(state, playerId, intersectionId, setupBuild);
        updateWinner();
    }

    public void upgradeLaboratory(String playerId, String intersectionId) {
        requireStarted();
        buildService.upgradeLaboratory(state, playerId, intersectionId);
        updateWinner();
    }

    public void endTurn() {
        requireStarted();
        updateWinner();

        if (!state.isGameOver()) {
            turnManager.endTurn();
        }
    }

    public GameState getState() {
        requireStarted();
        return state;
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

    private void requireStarted() {
        if (state == null) {
            throw new IllegalStateException("Game has not started");
        }
    }
}
