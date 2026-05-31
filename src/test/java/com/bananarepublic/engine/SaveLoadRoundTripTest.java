package com.bananarepublic.engine;

import com.bananarepublic.model.board.Board;
import com.bananarepublic.model.board.HexTile;
import com.bananarepublic.model.board.Intersection;
import com.bananarepublic.model.board.Path;
import com.bananarepublic.model.card.DevelopmentCard;
import com.bananarepublic.model.card.DevelopmentDeck;
import com.bananarepublic.model.card.KnightCard;
import com.bananarepublic.model.card.MonopolyCard;
import com.bananarepublic.model.card.RoadBuildingCard;
import com.bananarepublic.model.card.VictoryPointCard;
import com.bananarepublic.model.player.Player;
import com.bananarepublic.model.player.PlayerColor;
import com.bananarepublic.model.player.SpecialCardType;
import com.bananarepublic.model.resource.ResourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SaveLoadRoundTripTest {
    @TempDir
    java.nio.file.Path tempDir;

    @Test
    void jsonRoundTripPreservesComplexState() {
        GameEngine originalEngine = createConfiguredGame();
        java.io.File saveFile = tempDir.resolve("complex-state.json").toFile();

        originalEngine.saveGame(saveFile);

        GameEngine restoredEngine = new GameEngine();
        restoredEngine.loadGame(saveFile);

        assertGameStatesEquivalent(originalEngine.getState(), restoredEngine.getState());

        restoredEngine.endTurn();
        assertEquals("P3", restoredEngine.getState().getCurrentPlayer().getId());
        assertEquals(TurnPhase.RESOURCE_GATHERING, restoredEngine.getState().getTurnState().getPhase());
    }

    @Test
    void serializedRoundTripPreservesComplexState() {
        GameEngine originalEngine = createConfiguredGame();
        java.io.File saveFile = tempDir.resolve("complex-state.ser").toFile();

        originalEngine.saveGame(saveFile);

        GameEngine restoredEngine = new GameEngine();
        restoredEngine.loadGame(saveFile);

        assertGameStatesEquivalent(originalEngine.getState(), restoredEngine.getState());
        assertFalse(restoredEngine.getState().isGameOver());
    }

    private GameEngine createConfiguredGame() {
        GameEngine engine = new GameEngine();
        engine.startNewGame(new GameConfig(List.of(
                new PlayerConfig("Stewart", PlayerColor.RED),
                new PlayerConfig("Gro", PlayerColor.BLUE),
                new PlayerConfig("Kebin", PlayerColor.GREEN)
        ), BoardMode.FIXED, true));

        GameState state = engine.getState();
        Board board = state.getBoard();
        Player p1 = state.getPlayers().get(0);
        Player p2 = state.getPlayers().get(1);
        Player p3 = state.getPlayers().get(2);

        Intersection p1Intersection = firstValidSetupIntersection(board);
        Path p1Path = firstUnusedPath(p1Intersection);
        engine.buildWatchPost(p1.getId(), p1Intersection.getId(), true);
        engine.buildRoad(p1.getId(), p1Path.getId(), true);

        Intersection p2Intersection = firstValidSetupIntersection(board);
        Path p2Path = firstUnusedPath(p2Intersection);
        engine.buildWatchPost(p2.getId(), p2Intersection.getId(), true);
        engine.buildRoad(p2.getId(), p2Path.getId(), true);

        Intersection p3Intersection = firstValidSetupIntersection(board);
        Path p3Path = firstUnusedPath(p3Intersection);
        engine.buildWatchPost(p3.getId(), p3Intersection.getId(), true);
        engine.buildRoad(p3.getId(), p3Path.getId(), true);

        p1.addResource(ResourceType.ORE, 3);
        p1.addResource(ResourceType.WHEAT, 2);
        state.getTurnState().setCurrentPlayerIndex(0);
        state.getTurnState().setPhase(TurnPhase.TRADE_BUILD);
        engine.upgradeLaboratory(p1.getId(), p1Intersection.getId());

        p1.addResource(ResourceType.WOOD, 2);
        p1.addResource(ResourceType.BRICK, 1);
        p1.addResource(ResourceType.BANANA, 1);
        p2.addResource(ResourceType.BANANA, 3);
        p2.addResource(ResourceType.ORE, 1);
        p3.addResource(ResourceType.WHEAT, 4);
        p3.addResource(ResourceType.WOOD, 1);

        p1.addSecretVictoryPoints(2);
        p2.addSecretVictoryPoints(1);
        p1.addSpecialCard(SpecialCardType.LONGEST_ROAD);
        p2.addSpecialCard(SpecialCardType.LARGEST_ARMY);
        state.setLongestRoadHolder(p1);
        state.setLargestArmyHolder(p2);

        p1.addCard(new VictoryPointCard("VP-HAND"));
        p2.addCard(new RoadBuildingCard("ROAD-HAND"));
        p3.addCard(new MonopolyCard("MONO-HAND"));

        DevelopmentDeck customDeck = new DevelopmentDeck(List.of(
                new KnightCard("KNIGHT-DRAW-1"),
                new RoadBuildingCard("ROAD-DRAW-1"),
                new MonopolyCard("MONO-DRAW-1")
        ));
        customDeck.discard(new KnightCard("KNIGHT-DISCARD-1"));
        customDeck.discard(new VictoryPointCard("VP-DISCARD-1", true));
        state.setDevelopmentDeck(customDeck);

        state.getBank().take(ResourceType.WOOD, 2);
        state.getBank().take(ResourceType.BANANA, 5);
        state.getBank().take(ResourceType.WHEAT, 1);

        HexTile targetTile = board.getTiles().stream()
                .filter(tile -> tile.getTerrainType().producesResource())
                .findFirst()
                .orElseThrow();
        state.moveNimonTo(targetTile.getId());

        TurnState turnState = state.getTurnState();
        turnState.setCurrentPlayerIndex(1);
        turnState.setPhase(TurnPhase.TRADE_BUILD);
        turnState.setSetupRound(0);
        turnState.setHasRolledDice(true);
        turnState.setHasPlayedDevelopmentCard(true);
        turnState.setRemainingSeconds(42);
        turnState.addNewlyBoughtCard("ROAD-HAND");
        turnState.setPendingDiscardPlayerIds(Set.of("P1", "P3"));
        turnState.setNimonMovedThisSeven(true);

        return engine;
    }

    private void assertGameStatesEquivalent(GameState expected, GameState actual) {
        assertEquals(expected.getNimonTileId(), actual.getNimonTileId());
        assertEquals(expected.getCurrentPlayer().getId(), actual.getCurrentPlayer().getId());
        assertEquals(expected.getTurnState().getCurrentPlayerIndex(), actual.getTurnState().getCurrentPlayerIndex());
        assertEquals(expected.getTurnState().getPhase(), actual.getTurnState().getPhase());
        assertEquals(expected.getTurnState().getSetupRound(), actual.getTurnState().getSetupRound());
        assertEquals(expected.getTurnState().hasRolledDice(), actual.getTurnState().hasRolledDice());
        assertEquals(expected.getTurnState().hasPlayedDevelopmentCard(), actual.getTurnState().hasPlayedDevelopmentCard());
        assertEquals(expected.getTurnState().getRemainingSeconds(), actual.getTurnState().getRemainingSeconds());
        assertEquals(expected.getTurnState().isWaitingForSetupPipe(), actual.getTurnState().isWaitingForSetupPipe());
        assertEquals(expected.getTurnState().getSetupPostIntersectionId(), actual.getTurnState().getSetupPostIntersectionId());
        assertEquals(expected.getTurnState().getNewlyBoughtCardIds(), actual.getTurnState().getNewlyBoughtCardIds());
        assertEquals(expected.getTurnState().getPendingDiscardPlayerIds(), actual.getTurnState().getPendingDiscardPlayerIds());
        assertEquals(expected.getTurnState().isNimonMovedThisSeven(), actual.getTurnState().isNimonMovedThisSeven());
        assertEquals(expected.getWinner().map(Player::getId), actual.getWinner().map(Player::getId));
        assertEquals(expected.getLongestRoadHolder().map(Player::getId), actual.getLongestRoadHolder().map(Player::getId));
        assertEquals(expected.getLargestArmyHolder().map(Player::getId), actual.getLargestArmyHolder().map(Player::getId));

        assertEquals(resourceMap(expected.getBank()), resourceMap(actual.getBank()));
        assertEquals(cardSignatures(expected.getDevelopmentDeck().getDrawPile()), cardSignatures(actual.getDevelopmentDeck().getDrawPile()));
        assertEquals(cardSignatures(expected.getDevelopmentDeck().getDiscardPile()), cardSignatures(actual.getDevelopmentDeck().getDiscardPile()));

        assertEquals(tileSummary(expected.getBoard()), tileSummary(actual.getBoard()));
        assertEquals(intersectionSummary(expected.getBoard()), intersectionSummary(actual.getBoard()));
        assertEquals(pathSummary(expected.getBoard()), pathSummary(actual.getBoard()));
        assertEquals(harborSummary(expected.getBoard()), harborSummary(actual.getBoard()));

        assertEquals(expected.getPlayers().size(), actual.getPlayers().size());
        for (int i = 0; i < expected.getPlayers().size(); i++) {
            assertPlayerEquivalent(expected.getPlayers().get(i), actual.getPlayers().get(i));
        }
    }

    private void assertPlayerEquivalent(Player expected, Player actual) {
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getColor(), actual.getColor());
        assertEquals(resourceMap(expected), resourceMap(actual));
        assertEquals(expected.getSupply().getRemainingMonitoringPosts(), actual.getSupply().getRemainingMonitoringPosts());
        assertEquals(expected.getSupply().getRemainingLaboratories(), actual.getSupply().getRemainingLaboratories());
        assertEquals(expected.getSupply().getRemainingPipes(), actual.getSupply().getRemainingPipes());
        assertEquals(expected.getPlayedKnightCount(), actual.getPlayedKnightCount());
        assertEquals(expected.getSecretVictoryPoints(), actual.getSecretVictoryPoints());
        assertEquals(expected.getSpecialCards(), actual.getSpecialCards());
        assertEquals(cardSignatures(expected.getHandCards()), cardSignatures(actual.getHandCards()));
        assertEquals(
                expected.getOwnedBuildings().stream()
                        .map(building -> building.getLocation().getId() + ":" + building.getType())
                        .sorted()
                        .toList(),
                actual.getOwnedBuildings().stream()
                        .map(building -> building.getLocation().getId() + ":" + building.getType())
                        .sorted()
                        .toList()
        );
        assertEquals(
                expected.getOwnedPipes().stream()
                        .map(pipe -> pipe.getLocation().getId())
                        .sorted()
                        .toList(),
                actual.getOwnedPipes().stream()
                        .map(pipe -> pipe.getLocation().getId())
                        .sorted()
                        .toList()
        );
    }

    private Map<String, String> tileSummary(Board board) {
        return board.getTiles().stream()
                .sorted(Comparator.comparing(HexTile::getId))
                .collect(Collectors.toMap(
                        HexTile::getId,
                        tile -> tile.getTerrainType() + ":" + tile.getToken(),
                        (left, right) -> left,
                        java.util.LinkedHashMap::new
                ));
    }

    private Map<String, String> intersectionSummary(Board board) {
        return board.getIntersections().stream()
                .sorted(Comparator.comparing(Intersection::getId))
                .collect(Collectors.toMap(
                        Intersection::getId,
                        intersection -> intersection.getBuilding()
                                .map(building -> building.getOwner().getId() + ":" + building.getType())
                                .orElse("EMPTY"),
                        (left, right) -> left,
                        java.util.LinkedHashMap::new
                ));
    }

    private Map<String, String> pathSummary(Board board) {
        return board.getPaths().stream()
                .sorted(Comparator.comparing(Path::getId))
                .collect(Collectors.toMap(
                        Path::getId,
                        path -> path.getPipe()
                                .map(pipe -> pipe.getOwner().getId() + ":" + pipe.getId())
                                .orElse("EMPTY"),
                        (left, right) -> left,
                        java.util.LinkedHashMap::new
                ));
    }

    private Map<String, String> harborSummary(Board board) {
        return board.getHarbors().stream()
                .sorted(Comparator.comparing(com.bananarepublic.model.harbor.Harbor::getId))
                .collect(Collectors.toMap(
                        com.bananarepublic.model.harbor.Harbor::getId,
                        harbor -> harbor.getClass().getSimpleName() + ":" + harbor.getAttachedPath().getId(),
                        (left, right) -> left,
                        java.util.LinkedHashMap::new
                ));
    }

    private Map<ResourceType, Integer> resourceMap(Player player) {
        return resourceMap(player::getResourceAmount);
    }

    private Map<ResourceType, Integer> resourceMap(com.bananarepublic.model.resource.Bank bank) {
        return resourceMap(bank::getAmount);
    }

    private Map<ResourceType, Integer> resourceMap(java.util.function.Function<ResourceType, Integer> reader) {
        return java.util.Arrays.stream(ResourceType.values())
                .collect(Collectors.toMap(
                        type -> type,
                        reader::apply,
                        (left, right) -> left,
                        () -> new java.util.EnumMap<>(ResourceType.class)
                ));
    }

    private List<String> cardSignatures(List<? extends DevelopmentCard> cards) {
        return cards.stream()
                .map(card -> {
                    boolean consumed = card instanceof VictoryPointCard victoryPointCard && victoryPointCard.isConsumed();
                    return card.getId() + ":" + card.getClass().getSimpleName() + ":" + consumed;
                })
                .toList();
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

    private Path firstUnusedPath(Intersection intersection) {
        return intersection.getConnectedPaths().stream()
                .filter(path -> !path.hasPipe())
                .findFirst()
                .orElseThrow();
    }
}
