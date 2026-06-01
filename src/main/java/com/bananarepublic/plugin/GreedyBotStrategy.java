package com.bananarepublic.plugin;

import com.bananarepublic.engine.GameState;
import com.bananarepublic.model.board.HexTile;
import com.bananarepublic.model.board.Intersection;
import com.bananarepublic.model.board.Path;
import com.bananarepublic.model.player.Player;
import com.bananarepublic.model.resource.ResourceInventory;
import com.bananarepublic.model.resource.ResourceType;
import com.bananarepublic.service.build.BuildActionType;
import com.bananarepublic.service.build.BuildCostProvider;
import com.bananarepublic.service.build.BuildService;
import com.bananarepublic.service.trade.TradeService;

import java.util.Comparator;
import java.util.List;

public final class GreedyBotStrategy implements PlayerStrategy {
    private final BuildService buildService = new BuildService();
    private final BuildCostProvider costProvider = new BuildCostProvider();
    private final TradeService tradeService = new TradeService();

    @Override
    public Action takeTurn(GameState state) {
        Player player = state.getCurrentPlayer();
        String playerId = player.getId();

        List<String> labIds = validLaboratoryUpgrades(state, playerId);
        if (!labIds.isEmpty() && canAfford(player, BuildActionType.LABORATORY)) {
            return new BotActions.UpgradeLaboratoryAction(bestLaboratoryUpgrade(state, labIds));
        }

        List<String> postIds = validMonitoringPosts(state, playerId);
        if (!postIds.isEmpty() && canAfford(player, BuildActionType.MONITORING_POST)) {
            return new BotActions.BuildMonitoringPostAction(bestMonitoringPost(state, postIds));
        }

        List<String> pathIds = validPipes(state, playerId);
        if (!pathIds.isEmpty() && canAfford(player, BuildActionType.PIPE)) {
            return new BotActions.BuildPipeAction(bestPipe(player, pathIds, state));
        }

        if (hasDevelopmentDeck(state) && canAfford(player, BuildActionType.EXPERIMENT_CARD)) {
            return new BotActions.BuyDevelopmentCardAction();
        }

        Action tradeForLab = chooseMaritimeTrade(state, player, BuildActionType.LABORATORY, !labIds.isEmpty());
        if (tradeForLab != null) {
            return tradeForLab;
        }

        Action tradeForPost = chooseMaritimeTrade(state, player, BuildActionType.MONITORING_POST, !postIds.isEmpty());
        if (tradeForPost != null) {
            return tradeForPost;
        }

        Action tradeForPipe = chooseMaritimeTrade(state, player, BuildActionType.PIPE, !pathIds.isEmpty());
        if (tradeForPipe != null) {
            return tradeForPipe;
        }

        Action tradeForCard = chooseMaritimeTrade(state, player, BuildActionType.EXPERIMENT_CARD, hasDevelopmentDeck(state));
        if (tradeForCard != null) {
            return tradeForCard;
        }

        return new BotActions.EndTurnAction();
    }

    private List<String> validLaboratoryUpgrades(GameState state, String playerId) {
        return state.getBoard().getIntersections().stream()
                .map(Intersection::getId)
                .filter(id -> buildService.canUpgradeLaboratory(state, playerId, id))
                .toList();
    }

    private List<String> validMonitoringPosts(GameState state, String playerId) {
        return state.getBoard().getIntersections().stream()
                .map(Intersection::getId)
                .filter(id -> buildService.canBuildMonitoringPost(state, playerId, id, false))
                .toList();
    }

    private List<String> validPipes(GameState state, String playerId) {
        return state.getBoard().getPaths().stream()
                .map(Path::getId)
                .filter(id -> buildService.canBuildPipe(state, playerId, id, false))
                .toList();
    }

    private boolean hasDevelopmentDeck(GameState state) {
        return state.getDevelopmentDeck() != null && !state.getDevelopmentDeck().isEmpty();
    }

    private boolean canAfford(Player player, BuildActionType actionType) {
        return player.hasResources(costProvider.getCost(actionType));
    }

    private String bestLaboratoryUpgrade(GameState state, List<String> ids) {
        return ids.stream()
                .max(Comparator.comparingDouble(id -> intersectionValue(state.getBoard().getIntersection(id))))
                .orElseThrow();
    }

    private String bestMonitoringPost(GameState state, List<String> ids) {
        return ids.stream()
                .max(Comparator.comparingDouble(id -> intersectionValue(state.getBoard().getIntersection(id)) + harborBonus(state.getBoard().getIntersection(id))))
                .orElseThrow();
    }

    private String bestPipe(Player player, List<String> ids, GameState state) {
        return ids.stream()
                .max(Comparator.comparingDouble(id -> pipeValue(player, state.getBoard().getPath(id))))
                .orElseThrow();
    }

    private Action chooseMaritimeTrade(GameState state, Player player, BuildActionType target, boolean targetAvailable) {
        if (!targetAvailable) {
            return null;
        }

        ResourceInventory cost = costProvider.getCost(target);
        ResourceInventory current = player.getResourceInventoryCopy();
        MaritimeCandidate best = null;

        for (ResourceType requestedType : ResourceType.values()) {
            int missing = cost.getAmount(requestedType) - current.getAmount(requestedType);
            if (missing <= 0 || !state.getBank().hasResource(requestedType, 1)) {
                continue;
            }

            for (ResourceType offeredType : ResourceType.values()) {
                if (offeredType == requestedType) {
                    continue;
                }

                int ratio = tradeService.resolveBestRatio(state, player, offeredType);
                int reserve = cost.getAmount(offeredType);
                int available = current.getAmount(offeredType);
                if (available - ratio < reserve) {
                    continue;
                }

                ResourceInventory afterTrade = current.copy();
                afterTrade.remove(offeredType, ratio);
                afterTrade.add(requestedType, 1);

                boolean completesTarget = afterTrade.hasEnough(cost);
                int score = (completesTarget ? 1000 : 0)
                        + (5 - ratio) * 100
                        + Math.max(0, available - reserve - ratio) * 5
                        + tileResourcePriority(requestedType);

                MaritimeCandidate candidate = new MaritimeCandidate(offeredType, ratio, requestedType, score);
                if (best == null || candidate.score() > best.score()) {
                    best = candidate;
                }
            }
        }

        if (best == null) {
            return null;
        }
        return new BotActions.MaritimeTradeAction(best.offeredType(), best.offeredAmount(), best.requestedType());
    }

    private double pipeValue(Player player, Path path) {
        double score = 0;
        score += endpointValue(path.getEndpointA(), player);
        score += endpointValue(path.getEndpointB(), player);
        if (path.getHarbor().isPresent()) {
            score += 4;
        }
        if (path.isCoastalPath()) {
            score += 1.5;
        }
        return score;
    }

    private double endpointValue(Intersection intersection, Player player) {
        double score = 0;
        if (intersection.getBuilding().isEmpty() && hasEnoughDistance(intersection)) {
            score += 7;
        }
        score += intersectionValue(intersection);
        score += harborBonus(intersection);
        score += intersection.getConnectedPaths().stream()
                .flatMap(path -> path.getPipe().stream())
                .filter(pipe -> pipe.isOwnedBy(player))
                .count() * 1.5;
        return score;
    }

    private boolean hasEnoughDistance(Intersection intersection) {
        return intersection.getConnectedPaths().stream()
                .map(path -> path.getOtherEndpoint(intersection))
                .noneMatch(Intersection::isOccupied);
    }

    private double harborBonus(Intersection intersection) {
        return intersection.getConnectedPaths().stream()
                .map(Path::getHarbor)
                .flatMap(java.util.Optional::stream)
                .mapToDouble(harbor -> harbor.getRatio() == 2 ? 3.5 : 2.0)
                .sum();
    }

    private double intersectionValue(Intersection intersection) {
        return intersection.getAdjacentTiles().stream()
                .mapToDouble(this::tileValue)
                .sum();
    }

    private double tileValue(HexTile tile) {
        if (!tile.getTerrainType().producesResource() || tile.getToken() == null) {
            return 0;
        }
        return switch (tile.getToken()) {
            case 6, 8 -> 5.0;
            case 5, 9 -> 4.0;
            case 4, 10 -> 3.0;
            case 3, 11 -> 2.0;
            case 2, 12 -> 1.0;
            default -> 0.5;
        } + tileResourcePriority(tile.getProducedResource()) * 0.1;
    }

    private int tileResourcePriority(ResourceType type) {
        return switch (type) {
            case ORE -> 5;
            case WHEAT -> 4;
            case BANANA -> 3;
            case BRICK -> 2;
            case WOOD -> 1;
        };
    }

    private record MaritimeCandidate(
            ResourceType offeredType,
            int offeredAmount,
            ResourceType requestedType,
            int score
    ) {}
}
