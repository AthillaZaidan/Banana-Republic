package com.bananarepublic.engine;

import com.bananarepublic.exception.InvalidTradeException;
import com.bananarepublic.model.board.HexTile;
import com.bananarepublic.model.board.Intersection;
import com.bananarepublic.model.building.MonitoringPost;
import com.bananarepublic.model.card.KnightCard;
import com.bananarepublic.model.harbor.Harbor;
import com.bananarepublic.model.player.Player;
import com.bananarepublic.model.player.PlayerColor;
import com.bananarepublic.model.resource.ResourceInventory;
import com.bananarepublic.model.resource.ResourceType;
import com.bananarepublic.service.dice.DiceMode;
import com.bananarepublic.service.dice.DiceRoll;
import com.bananarepublic.service.trade.MaritimeTradeRequest;
import com.bananarepublic.service.trade.TradeOffer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TradeAndNimonFlowTest {
    @Test
    void domesticTradeAcceptChangesBothPlayersResources() {
        GameEngine engine = createTwoPlayerGameInTradeBuild();
        Player p1 = engine.getState().getPlayers().get(0);
        Player p2 = engine.getState().getPlayers().get(1);
        p1.addResource(ResourceType.WOOD, 2);
        p2.addResource(ResourceType.BRICK, 2);
        int p1WoodBefore = p1.getResourceAmount(ResourceType.WOOD);
        int p1BrickBefore = p1.getResourceAmount(ResourceType.BRICK);
        int p2WoodBefore = p2.getResourceAmount(ResourceType.WOOD);
        int p2BrickBefore = p2.getResourceAmount(ResourceType.BRICK);

        ResourceInventory offered = new ResourceInventory();
        offered.add(ResourceType.WOOD, 1);
        ResourceInventory requested = new ResourceInventory();
        requested.add(ResourceType.BRICK, 1);

        engine.submitDomesticTrade(new TradeOffer(p1.getId(), p2.getId(), offered, requested));
        engine.acceptDomesticTrade(p2.getId());

        assertEquals(p1WoodBefore - 1, p1.getResourceAmount(ResourceType.WOOD));
        assertEquals(p1BrickBefore + 1, p1.getResourceAmount(ResourceType.BRICK));
        assertEquals(p2BrickBefore - 1, p2.getResourceAmount(ResourceType.BRICK));
        assertEquals(p2WoodBefore + 1, p2.getResourceAmount(ResourceType.WOOD));
    }

    @Test
    void domesticTradeRejectAndCounterFlowWork() {
        GameEngine engine = createTwoPlayerGameInTradeBuild();
        Player p1 = engine.getState().getPlayers().get(0);
        Player p2 = engine.getState().getPlayers().get(1);
        p1.addResource(ResourceType.WOOD, 2);
        p2.addResource(ResourceType.BRICK, 2);
        p2.addResource(ResourceType.BANANA, 1);

        ResourceInventory offered = new ResourceInventory();
        offered.add(ResourceType.WOOD, 1);
        ResourceInventory requested = new ResourceInventory();
        requested.add(ResourceType.BRICK, 1);

        engine.submitDomesticTrade(new TradeOffer(p1.getId(), p2.getId(), offered, requested));
        ResourceInventory counterOffered = new ResourceInventory();
        counterOffered.add(ResourceType.BRICK, 1);
        ResourceInventory counterRequested = new ResourceInventory();
        counterRequested.add(ResourceType.WOOD, 1);
        counterRequested.add(ResourceType.BANANA, 1);
        engine.counterDomesticTrade(p2.getId(), new TradeOffer(p2.getId(), p1.getId(), counterOffered, counterRequested));
        assertThrows(InvalidTradeException.class, () -> engine.acceptDomesticTrade(p2.getId()));
        engine.rejectDomesticTrade(p1.getId());
    }

    @Test
    void invalidDomesticTradeThrowsDomainException() {
        GameEngine engine = createTwoPlayerGameInTradeBuild();
        Player p1 = engine.getState().getPlayers().get(0);
        Player p2 = engine.getState().getPlayers().get(1);
        p1.addResource(ResourceType.WOOD, 1);
        p2.addResource(ResourceType.WOOD, 1);

        ResourceInventory offered = new ResourceInventory();
        offered.add(ResourceType.WOOD, 1);
        ResourceInventory requested = new ResourceInventory();
        requested.add(ResourceType.WOOD, 1);

        assertThrows(InvalidTradeException.class, () ->
                engine.submitDomesticTrade(new TradeOffer(p1.getId(), p2.getId(), offered, requested)));
    }

    @Test
    void maritimeTradeUsesBestRatioAndUpdatesBank() {
        GameEngine engine = createTwoPlayerGameInTradeBuild();
        Player active = engine.getState().getCurrentPlayer();
        active.addResource(ResourceType.WOOD, 10);
        active.addResource(ResourceType.BRICK, 10);
        active.addResource(ResourceType.WHEAT, 10);
        int oreBankBefore = engine.getState().getBank().getAmount(ResourceType.ORE);

        int initialRatio = engine.getBestMaritimeRatio(active.getId(), ResourceType.WOOD);
        engine.submitMaritimeTrade(new MaritimeTradeRequest(
                active.getId(), ResourceType.WOOD, initialRatio, ResourceType.ORE
        ));
        assertEquals(oreBankBefore - 1, engine.getState().getBank().getAmount(ResourceType.ORE));

        Harbor generic = engine.getState().getBoard().getHarbors().stream()
                .filter(harbor -> harbor.getRatio() == 3)
                .findFirst()
                .orElseThrow();
        grantHarborAccess(active, generic);
        assertEquals(3, engine.getBestMaritimeRatio(active.getId(), ResourceType.BRICK));
        engine.submitMaritimeTrade(new MaritimeTradeRequest(
                active.getId(), ResourceType.BRICK, 3, ResourceType.ORE
        ));

        Harbor specific = engine.getState().getBoard().getHarbors().stream()
                .filter(harbor -> harbor.getRatio() == 2 && harbor.canTradeWith(ResourceType.WHEAT))
                .findFirst()
                .orElseThrow();
        grantHarborAccess(active, specific);
        assertEquals(2, engine.getBestMaritimeRatio(active.getId(), ResourceType.WHEAT));
        engine.submitMaritimeTrade(new MaritimeTradeRequest(
                active.getId(), ResourceType.WHEAT, 2, ResourceType.ORE
        ));
    }

    @Test
    void sevenFlowEnforcesDiscardMoveAndStealRules() {
        GameEngine engine = createTwoPlayerGameAndFinishSetup();
        GameState state = engine.getState();
        Player active = state.getCurrentPlayer();
        Player other = state.getPlayers().get(1);

        active.addResource(ResourceType.WOOD, 8);
        other.addResource(ResourceType.BRICK, 8);
        other.addResource(ResourceType.WHEAT, 1);

        engine.rollDice(DiceMode.MANUAL, DiceRoll.of(3, 4));
        assertEquals(TurnPhase.DISCARD, state.getTurnState().getPhase());

        ResourceInventory badDiscard = new ResourceInventory();
        badDiscard.add(ResourceType.WOOD, 3);
        assertThrows(RuntimeException.class, () -> engine.discardForSeven(active.getId(), badDiscard));

        ResourceInventory discardActive = new ResourceInventory();
        discardActive.add(ResourceType.WOOD, active.getTotalResourceCards() / 2);
        engine.discardForSeven(active.getId(), discardActive);

        ResourceInventory discardOther = new ResourceInventory();
        discardOther.add(ResourceType.BRICK, other.getTotalResourceCards() / 2);
        engine.discardForSeven(other.getId(), discardOther);
        assertEquals(TurnPhase.MOVE_NIMON_UNGU, state.getTurnState().getPhase());

        assertThrows(RuntimeException.class, () -> engine.moveNimonAfterSeven(state.getNimonTileId()));
        String targetTileId = state.getBoard().getTiles().stream()
                .map(HexTile::getId)
                .filter(id -> !id.equals(state.getNimonTileId()))
                .findFirst()
                .orElseThrow();
        engine.moveNimonAfterSeven(targetTileId);
        assertEquals(targetTileId, state.getNimonTileId());

        assertTrue(engine.getValidStealTargetsAfterSeven().stream().noneMatch(player -> player.equals(active)));
        if (!engine.getValidStealTargetsAfterSeven().isEmpty()) {
            String victimId = engine.getValidStealTargetsAfterSeven().getFirst().getId();
            int victimBefore = state.getPlayerById(victimId).getTotalResourceCards();
            int activeBefore = active.getTotalResourceCards();
            engine.stealAfterSeven(victimId);
            assertEquals(victimBefore - 1, state.getPlayerById(victimId).getTotalResourceCards());
            assertEquals(activeBefore + 1, active.getTotalResourceCards());
        } else {
            engine.finishNimonAfterSevenWithoutSteal();
        }
        assertEquals(TurnPhase.TRADE_BUILD, state.getTurnState().getPhase());
    }

    @Test
    void knightCardRequiresAValidAdjacentVictim() {
        GameEngine engine = createTwoPlayerGameInTradeBuild();
        GameState state = engine.getState();
        Player active = state.getCurrentPlayer();
        Player other = state.getPlayers().stream()
                .filter(player -> !player.equals(active))
                .findFirst()
                .orElseThrow();
        other.addResource(ResourceType.WOOD, 1);

        KnightCard knightCard = new KnightCard("KNIGHT-HAND");
        active.addCard(knightCard);

        String targetTileId = other.getOwnedBuildings().stream()
                .flatMap(building -> building.getLocation().getAdjacentTiles().stream())
                .map(HexTile::getId)
                .filter(id -> !id.equals(state.getNimonTileId()))
                .findFirst()
                .orElseThrow();

        assertThrows(RuntimeException.class, () ->
                engine.playDevelopmentCard(active.getId(), knightCard.getId(), targetTileId, active.getId()));

        int otherBefore = other.getTotalResourceCards();
        int activeBefore = active.getTotalResourceCards();
        engine.playDevelopmentCard(active.getId(), knightCard.getId(), targetTileId, other.getId());

        assertEquals(otherBefore - 1, other.getTotalResourceCards());
        assertEquals(activeBefore + 1, active.getTotalResourceCards());
    }

    private GameEngine createTwoPlayerGameInTradeBuild() {
        GameEngine engine = createTwoPlayerGameAndFinishSetup();
        engine.rollDice(DiceMode.MANUAL, DiceRoll.of(2, 4));
        return engine;
    }

    private GameEngine createTwoPlayerGameAndFinishSetup() {
        GameEngine engine = new GameEngine();
        engine.startNewGame(new GameConfig(List.of(
                new PlayerConfig("Nimo", PlayerColor.RED),
                new PlayerConfig("Nero", PlayerColor.BLUE)
        ), BoardMode.FIXED, true));

        int setupTurns = engine.getState().getPlayers().size() * 2;
        for (int i = 0; i < setupTurns; i++) {
            placeSetupPair(engine, engine.getState().getCurrentPlayer().getId());
        }
        return engine;
    }

    private void placeSetupPair(GameEngine engine, String playerId) {
        Intersection intersection = engine.getState().getBoard().getIntersections().stream()
                .filter(candidate -> !candidate.isOccupied())
                .filter(candidate -> candidate.getConnectedPaths().stream()
                        .map(path -> path.getOtherEndpoint(candidate))
                        .noneMatch(Intersection::isOccupied))
                .findFirst()
                .orElseThrow();

        String pathId = intersection.getConnectedPaths().getFirst().getId();
        engine.placeSetupWatchPost(playerId, intersection.getId());
        engine.placeSetupRoad(playerId, pathId);
    }

    private void grantHarborAccess(Player player, Harbor harbor) {
        Intersection a = harbor.getAttachedPath().getEndpointA();
        Intersection b = harbor.getAttachedPath().getEndpointB();
        Intersection target = !a.isOccupied() ? a : (!b.isOccupied() ? b : null);
        if (target == null) {
            return;
        }
        MonitoringPost post = new MonitoringPost(player, target);
        target.placeBuilding(post);
        player.registerBuilding(post);
    }
}
