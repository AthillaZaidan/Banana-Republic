package com.bananarepublic.plugin;

import com.bananarepublic.engine.GameEngine;
import com.bananarepublic.model.resource.ResourceType;
import com.bananarepublic.service.trade.MaritimeTradeRequest;

public final class BotActions {
    private BotActions() {}

    public record EndTurnAction() implements Action {
        @Override
        public void execute(GameEngine engine, String playerId) {
            engine.endTurn();
        }

        @Override
        public String describe() {
            return "end the turn";
        }
    }

    public record BuyDevelopmentCardAction() implements Action {
        @Override
        public void execute(GameEngine engine, String playerId) {
            engine.buyDevelopmentCard(playerId);
        }

        @Override
        public String describe() {
            return "buy a development card";
        }
    }

    public record BuildPipeAction(String pathId) implements Action {
        @Override
        public void execute(GameEngine engine, String playerId) {
            engine.buildRoad(playerId, pathId);
        }

        @Override
        public String describe() {
            return "build a pipe on " + pathId;
        }
    }

    public record BuildMonitoringPostAction(String intersectionId) implements Action {
        @Override
        public void execute(GameEngine engine, String playerId) {
            engine.buildWatchPost(playerId, intersectionId);
        }

        @Override
        public String describe() {
            return "build a monitoring post at " + intersectionId;
        }
    }

    public record UpgradeLaboratoryAction(String intersectionId) implements Action {
        @Override
        public void execute(GameEngine engine, String playerId) {
            engine.upgradeLaboratory(playerId, intersectionId);
        }

        @Override
        public String describe() {
            return "upgrade a laboratory at " + intersectionId;
        }
    }

    public record MaritimeTradeAction(
            ResourceType offeredType,
            int offeredAmount,
            ResourceType requestedType
    ) implements Action {
        @Override
        public void execute(GameEngine engine, String playerId) {
            engine.submitMaritimeTrade(new MaritimeTradeRequest(
                    playerId,
                    offeredType,
                    offeredAmount,
                    requestedType
            ));
        }

        @Override
        public String describe() {
            return "trade " + offeredAmount + " " + offeredType + " for " + requestedType;
        }
    }
}
