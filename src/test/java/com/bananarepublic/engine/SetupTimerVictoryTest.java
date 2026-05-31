package com.bananarepublic.engine;

import com.bananarepublic.model.board.Board;
import com.bananarepublic.model.board.HexTile;
import com.bananarepublic.model.board.Intersection;
import com.bananarepublic.model.board.Path;
import com.bananarepublic.model.player.Player;
import com.bananarepublic.model.player.PlayerColor;
import com.bananarepublic.model.player.SpecialCardType;
import com.bananarepublic.model.resource.Bank;
import com.bananarepublic.model.resource.ResourceType;
import com.bananarepublic.service.dice.DiceMode;
import com.bananarepublic.service.dice.DiceRoll;
import com.bananarepublic.service.timer.TurnTimerService;
import com.bananarepublic.service.victory.VictoryService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SetupTimerVictoryTest {
    @Test
    void setupRunsClockwiseThenCounterClockwiseAndGrantsSecondPostResources() {
        GameEngine engine = createThreePlayerGame();
        GameState state = engine.getState();

        assertEquals(TurnPhase.SETUP, state.getTurnState().getPhase());
        assertEquals("P1", state.getCurrentPlayer().getId());

        placeSetupPair(engine, "P1");
        assertEquals("P2", state.getCurrentPlayer().getId());

        placeSetupPair(engine, "P2");
        assertEquals("P3", state.getCurrentPlayer().getId());

        placeSetupPair(engine, "P3");
        assertEquals(2, state.getTurnState().getSetupRound());
        assertEquals("P3", state.getCurrentPlayer().getId());

        int before = state.getCurrentPlayer().getTotalResourceCards();
        placeSetupPair(engine, "P3");

        assertTrue(state.getPlayers().get(2).getTotalResourceCards() > before);
        assertEquals("P2", state.getCurrentPlayer().getId());

        placeSetupPair(engine, "P2");
        placeSetupPair(engine, "P1");

        assertEquals(TurnPhase.RESOURCE_GATHERING, state.getTurnState().getPhase());
        assertEquals("P1", state.getCurrentPlayer().getId());
    }

    @Test
    void timerCanExpireTradeBuildPhaseAndAutoEndTurn() {
        GameEngine engine = createTwoPlayerGame();
        finishSetup(engine);

        engine.rollDice(DiceMode.MANUAL, DiceRoll.of(3, 3));
        assertEquals(TurnPhase.TRADE_BUILD, engine.getState().getTurnState().getPhase());

        engine.startTurnTimer(90);
        engine.getTimerService().expireNow();

        assertEquals("P2", engine.getState().getCurrentPlayer().getId());
        assertEquals(TurnPhase.RESOURCE_GATHERING, engine.getState().getTurnState().getPhase());
        assertEquals(0, engine.getTimerService().getRemainingSeconds());
    }

    @Test
    void victoryCountsBuildingsSecretPointsAndSpecialCardsOnlyForCurrentPlayer() {
        GameEngine engine = createTwoPlayerGame();
        GameState state = engine.getState();
        Player current = state.getCurrentPlayer();
        Player other = state.getPlayers().get(1);

        current.addSecretVictoryPoints(2);
        current.addSpecialCard(SpecialCardType.LONGEST_ROAD);
        other.addSecretVictoryPoints(20);

        assertEquals(4, new VictoryService().calculateVictoryPoints(current));
        assertTrue(new VictoryService().findWinner(state).isEmpty());

        current.addSpecialCard(SpecialCardType.LARGEST_ARMY);
        current.addSecretVictoryPoints(4);

        engine.checkVictory();
        assertEquals(current, state.getWinner().orElseThrow());
        assertEquals(TurnPhase.GAME_OVER, state.getTurnState().getPhase());
    }

    @Test
    void largestArmyAndLongestRoadOwnershipCanTransfer() {
        GameEngine engine = createTwoPlayerGame();
        GameState state = engine.getState();
        Player p1 = state.getPlayers().get(0);
        Player p2 = state.getPlayers().get(1);

        engine.recordKnightPlayed(p1.getId());
        engine.recordKnightPlayed(p1.getId());
        engine.recordKnightPlayed(p1.getId());
        assertEquals(p1, state.getLargestArmyHolder().orElseThrow());
        assertTrue(p1.hasSpecialCard(SpecialCardType.LARGEST_ARMY));

        for (int i = 0; i < 4; i++) {
            engine.recordKnightPlayed(p2.getId());
        }
        assertEquals(p2, state.getLargestArmyHolder().orElseThrow());
        assertTrue(p2.hasSpecialCard(SpecialCardType.LARGEST_ARMY));

        finishSetup(engine);
        Player active = state.getCurrentPlayer();
        List<Path> road = connectedEmptyPathChain(state.getBoard(), 5);
        grantRoadResources(active, 5);

        for (Path path : road) {
            engine.buildRoad(active.getId(), path.getId(), true);
        }

        assertEquals(active, state.getLongestRoadHolder().orElseThrow());
        assertTrue(active.hasSpecialCard(SpecialCardType.LONGEST_ROAD));
    }

    @Test
    void buildActionsRequireTradeBuildPhaseAndActivePlayer() {
        GameEngine engine = createTwoPlayerGame();
        Player p1 = engine.getState().getPlayers().get(0);
        Player p2 = engine.getState().getPlayers().get(1);
        Path setupPath = engine.getState().getBoard().getPaths().stream()
                .findFirst()
                .orElseThrow();

        assertThrows(IllegalStateException.class, () -> engine.buildRoad(p1.getId(), setupPath.getId()));

        finishSetup(engine);
        Player active = engine.getState().getCurrentPlayer();
        grantRoadResources(active, 1);
        engine.rollDice(DiceMode.MANUAL, DiceRoll.of(2, 4));

        Path buildPath = connectedEmptyPathChain(engine.getState().getBoard(), 1).getFirst();
        String nonActivePlayerId = active.equals(p1) ? p2.getId() : p1.getId();
        assertThrows(IllegalArgumentException.class, () -> engine.buildRoad(nonActivePlayerId, buildPath.getId()));
    }

    @Test
    void opponentBuildingBreaksLongestRoadContinuity() {
        GameEngine engine = createTwoPlayerGame();
        GameState state = engine.getState();
        finishSetup(engine);

        Player active = state.getCurrentPlayer();
        Player blocker = state.getPlayers().stream()
                .filter(player -> !player.equals(active))
                .findFirst()
                .orElseThrow();
        List<Path> road = connectedEmptyPathChain(state.getBoard(), 5);
        grantRoadResources(active, 5);

        for (Path path : road) {
            engine.buildRoad(active.getId(), path.getId(), true);
        }
        assertEquals(active, state.getLongestRoadHolder().orElseThrow());

        Intersection blockedIntersection = sharedIntersection(road.get(1), road.get(2));
        blockedIntersection.placeBuilding(new com.bananarepublic.model.building.MonitoringPost(blocker, blockedIntersection));
        blocker.registerBuilding(blockedIntersection.getBuilding().orElseThrow());

        engine.checkVictory();

        assertEquals(3, new VictoryService().calculateLongestRoad(active));
        assertTrue(state.getLongestRoadHolder().isEmpty());
        assertTrue(!active.hasSpecialCard(SpecialCardType.LONGEST_ROAD));
    }

    @Test
    void productionShortageGivesSinglePlayerRemainderButSkipsMultiplePlayers() {
        GameEngine engine = createTwoPlayerGame();
        GameState state = engine.getState();
        HexTile tile = state.getBoard().getTiles().stream()
                .filter(candidate -> candidate.getTerrainType().producesResource())
                .findFirst()
                .orElseThrow();

        Intersection first = tile.getIntersections().get(0);
        Intersection second = tile.getIntersections().stream()
                .filter(candidate -> candidate != first)
                .filter(candidate -> !candidate.getConnectedPaths().stream()
                        .map(path -> path.getOtherEndpoint(candidate))
                        .toList()
                        .contains(first))
                .findFirst()
                .orElseThrow();

        Player p1 = state.getPlayers().get(0);
        Player p2 = state.getPlayers().get(1);
        engine.buildWatchPost(p1.getId(), first.getId(), true);
        engine.buildWatchPost(p2.getId(), second.getId(), true);

        drainBank(state.getBank(), tile.getProducedResource(), 18);
        int bankBefore = state.getBank().getAmount(tile.getProducedResource());

        engine.produceResources(tile.getToken());

        assertEquals(bankBefore, state.getBank().getAmount(tile.getProducedResource()));
        assertEquals(0, p1.getResourceAmount(tile.getProducedResource()));
        assertEquals(0, p2.getResourceAmount(tile.getProducedResource()));
    }

    private GameEngine createTwoPlayerGame() {
        GameEngine engine = new GameEngine();
        engine.startNewGame(new GameConfig(List.of(
                new PlayerConfig("Nimo", PlayerColor.RED),
                new PlayerConfig("Nero", PlayerColor.BLUE)
        ), BoardMode.FIXED, true));
        return engine;
    }

    private GameEngine createThreePlayerGame() {
        GameEngine engine = new GameEngine();
        engine.startNewGame(new GameConfig(List.of(
                new PlayerConfig("Nimo", PlayerColor.RED),
                new PlayerConfig("Nero", PlayerColor.BLUE),
                new PlayerConfig("Jordy", PlayerColor.GREEN)
        ), BoardMode.FIXED, true));
        return engine;
    }

    private void finishSetup(GameEngine engine) {
        int setupTurns = engine.getState().getPlayers().size() * 2;
        for (int i = 0; i < setupTurns; i++) {
            placeSetupPair(engine, engine.getState().getCurrentPlayer().getId());
        }
    }

    private void placeSetupPair(GameEngine engine, String playerId) {
        Intersection intersection = firstValidSetupIntersection(engine.getState().getBoard());
        Path path = intersection.getConnectedPaths().getFirst();

        engine.placeSetupWatchPost(playerId, intersection.getId());
        engine.placeSetupRoad(playerId, path.getId());
    }

    private Intersection firstValidSetupIntersection(Board board) {
        return board.getIntersections().stream()
                .filter(intersection -> !intersection.isOccupied())
                .filter(intersection -> intersection.getConnectedPaths().stream()
                        .map(path -> path.getOtherEndpoint(intersection))
                        .noneMatch(Intersection::isOccupied))
                .findFirst()
                .orElseThrow();
    }

    private List<Path> connectedEmptyPathChain(Board board, int length) {
        for (Path start : board.getPaths()) {
            List<Path> chain = collectPathChain(start, length);
            if (chain.size() == length) {
                return chain;
            }
        }
        throw new IllegalStateException("No path chain found");
    }

    private List<Path> collectPathChain(Path start, int length) {
        java.util.ArrayList<Path> chain = new java.util.ArrayList<>();
        Path current = start;
        Intersection cursor = start.getEndpointB();

        while (current != null && chain.size() < length) {
            if (current.hasPipe()) {
                break;
            }
            chain.add(current);
            Path previous = current;
            current = cursor.getConnectedPaths().stream()
                    .filter(path -> path != previous)
                    .filter(path -> !path.hasPipe())
                    .findFirst()
                    .orElse(null);
            if (current != null) {
                cursor = current.getOtherEndpoint(cursor);
            }
        }

        return chain;
    }

    private void grantRoadResources(Player player, int pathCount) {
        player.addResource(ResourceType.WOOD, pathCount);
        player.addResource(ResourceType.BRICK, pathCount);
    }

    private Intersection sharedIntersection(Path first, Path second) {
        if (first.connectsTo(second.getEndpointA())) {
            return second.getEndpointA();
        }
        if (first.connectsTo(second.getEndpointB())) {
            return second.getEndpointB();
        }
        throw new IllegalArgumentException("Paths do not share an intersection");
    }

    private void drainBank(Bank bank, ResourceType type, int amount) {
        bank.take(type, amount);
        assertNotEquals(0, bank.getAmount(type));
    }
}
