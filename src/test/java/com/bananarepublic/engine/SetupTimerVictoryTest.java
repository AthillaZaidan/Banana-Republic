package com.bananarepublic.engine;

import com.bananarepublic.model.board.Board;
import com.bananarepublic.model.board.HexTile;
import com.bananarepublic.model.board.Intersection;
import com.bananarepublic.model.board.Path;
import com.bananarepublic.model.card.DevelopmentDeck;
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
    private record BreakableChain(List<Path> paths, int splitIndex) {}

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
        List<Path> road = findBreakablePathChain(state.getBoard(), 5).paths();
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
        BreakableChain breakableChain = findBreakablePathChain(state.getBoard(), 5);
        List<Path> road = breakableChain.paths();
        grantRoadResources(active, 5);

        for (Path path : road) {
            engine.buildRoad(active.getId(), path.getId(), true);
        }
        assertEquals(active, state.getLongestRoadHolder().orElseThrow());

        Intersection blockedIntersection = sharedIntersection(
                road.get(breakableChain.splitIndex()),
                road.get(breakableChain.splitIndex() + 1)
        );
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

    @Test
    void gameConfigRejectsFewerThanThreePlayers() {
        assertThrows(IllegalArgumentException.class, () -> new GameConfig(List.of(
                new PlayerConfig("Nimo", PlayerColor.RED),
                new PlayerConfig("Nero", PlayerColor.BLUE)
        ), BoardMode.FIXED, true));
    }

    @Test
    void buyingDevelopmentCardRequiresTradeBuildPhase() {
        GameEngine engine = createTwoPlayerGame();
        engine.getState().setDevelopmentDeck(DevelopmentDeck.createDefaultDeck());
        Player firstActive = engine.getState().getCurrentPlayer();
        firstActive.addResource(ResourceType.ORE, 1);
        firstActive.addResource(ResourceType.BANANA, 1);
        firstActive.addResource(ResourceType.WHEAT, 1);

        assertThrows(IllegalStateException.class, () -> engine.buyDevelopmentCard(firstActive.getId()));

        finishSetup(engine);
        Player secondActive = engine.getState().getCurrentPlayer();
        secondActive.addResource(ResourceType.ORE, 1);
        secondActive.addResource(ResourceType.BANANA, 1);
        secondActive.addResource(ResourceType.WHEAT, 1);
        engine.rollDice(DiceMode.MANUAL, DiceRoll.of(2, 4));

        engine.buyDevelopmentCard(secondActive.getId());
        assertEquals(1, secondActive.getHandCardCount());
    }

    private GameEngine createTwoPlayerGame() {
        GameEngine engine = new GameEngine();
        engine.startNewGame(new GameConfig(List.of(
                new PlayerConfig("Nimo", PlayerColor.RED),
                new PlayerConfig("Nero", PlayerColor.BLUE),
                new PlayerConfig("Jordy", PlayerColor.GREEN)
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
            java.util.HashSet<Intersection> visitedFromA = new java.util.HashSet<>();
            visitedFromA.add(start.getEndpointA());
            List<Path> chain = collectPathChain(
                    start,
                    start.getEndpointA(),
                    length,
                    new java.util.ArrayList<>(),
                    visitedFromA
            );
            if (!chain.isEmpty()) {
                return chain;
            }
            java.util.HashSet<Intersection> visitedFromB = new java.util.HashSet<>();
            visitedFromB.add(start.getEndpointB());
            chain = collectPathChain(
                    start,
                    start.getEndpointB(),
                    length,
                    new java.util.ArrayList<>(),
                    visitedFromB
            );
            if (!chain.isEmpty()) {
                return chain;
            }
        }
        throw new IllegalStateException("No path chain found");
    }

    private BreakableChain findBreakablePathChain(Board board, int length) {
        for (List<String> candidateIds : allSimplePathChainIds(board, length)) {
            for (int splitIndex : List.of(1, 2)) {
                GameEngine verificationEngine = createTwoPlayerGame();
                finishSetup(verificationEngine);
                Player active = verificationEngine.getState().getCurrentPlayer();
                Player blocker = verificationEngine.getState().getPlayers().stream()
                        .filter(player -> !player.equals(active))
                        .findFirst()
                        .orElseThrow();
                grantRoadResources(active, length);

                List<Path> candidatePaths = candidateIds.stream()
                        .map(id -> verificationEngine.getState().getBoard().getPath(id))
                        .toList();
                if (candidatePaths.stream().anyMatch(java.util.Objects::isNull)) {
                    continue;
                }

                try {
                    for (Path path : candidatePaths) {
                        verificationEngine.buildRoad(active.getId(), path.getId(), true);
                    }
                } catch (RuntimeException ex) {
                    continue;
                }

                if (verificationEngine.getState().getLongestRoadHolder().map(Player::getId)
                        .filter(active.getId()::equals).isEmpty()) {
                    continue;
                }

                Intersection blockedIntersection = sharedIntersection(
                        candidatePaths.get(splitIndex),
                        candidatePaths.get(splitIndex + 1)
                );
                if (blockedIntersection.isOccupied()) {
                    continue;
                }

                blockedIntersection.placeBuilding(new com.bananarepublic.model.building.MonitoringPost(blocker, blockedIntersection));
                blocker.registerBuilding(blockedIntersection.getBuilding().orElseThrow());
                verificationEngine.checkVictory();

                if (new VictoryService().calculateLongestRoad(active) == 3
                        && verificationEngine.getState().getLongestRoadHolder().isEmpty()) {
                    List<Path> originalPaths = candidateIds.stream()
                            .map(board::getPath)
                            .toList();
                    return new BreakableChain(originalPaths, splitIndex);
                }
            }
        }
        throw new IllegalStateException("No breakable path chain found");
    }

    private List<Path> collectPathChain(
            Path current,
            Intersection origin,
            int targetLength,
            java.util.ArrayList<Path> chain,
            java.util.Set<Intersection> visitedIntersections
    ) {
        if (current.hasPipe() || chain.contains(current)) {
            return List.of();
        }

        chain.add(current);
        Intersection nextIntersection = current.getOtherEndpoint(origin);
        if (!visitedIntersections.add(nextIntersection)) {
            chain.removeLast();
            return List.of();
        }
        if (chain.size() == targetLength) {
            return List.copyOf(chain);
        }

        for (Path next : nextIntersection.getConnectedPaths()) {
            if (next == current || next.hasPipe()) {
                continue;
            }
            List<Path> result = collectPathChain(next, nextIntersection, targetLength, chain, visitedIntersections);
            if (!result.isEmpty()) {
                return result;
            }
        }

        visitedIntersections.remove(nextIntersection);
        chain.removeLast();
        return List.of();
    }

    private List<List<String>> allSimplePathChainIds(Board board, int length) {
        List<List<String>> result = new java.util.ArrayList<>();
        for (Path start : board.getPaths()) {
            java.util.HashSet<Intersection> visitedFromA = new java.util.HashSet<>();
            visitedFromA.add(start.getEndpointA());
            collectAllPathChains(start, start.getEndpointA(), length, new java.util.ArrayList<>(), visitedFromA, result);

            java.util.HashSet<Intersection> visitedFromB = new java.util.HashSet<>();
            visitedFromB.add(start.getEndpointB());
            collectAllPathChains(start, start.getEndpointB(), length, new java.util.ArrayList<>(), visitedFromB, result);
        }
        return result;
    }

    private void collectAllPathChains(
            Path current,
            Intersection origin,
            int targetLength,
            java.util.ArrayList<Path> chain,
            java.util.Set<Intersection> visitedIntersections,
            List<List<String>> result
    ) {
        if (current.hasPipe() || chain.contains(current)) {
            return;
        }

        chain.add(current);
        Intersection nextIntersection = current.getOtherEndpoint(origin);
        if (!visitedIntersections.add(nextIntersection)) {
            chain.removeLast();
            return;
        }
        if (chain.size() == targetLength) {
            result.add(chain.stream().map(Path::getId).toList());
            visitedIntersections.remove(nextIntersection);
            chain.removeLast();
            return;
        }

        for (Path next : nextIntersection.getConnectedPaths()) {
            if (next == current || next.hasPipe()) {
                continue;
            }
            collectAllPathChains(next, nextIntersection, targetLength, chain, visitedIntersections, result);
        }

        visitedIntersections.remove(nextIntersection);
        chain.removeLast();
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
