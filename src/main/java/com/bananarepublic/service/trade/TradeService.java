package com.bananarepublic.service.trade;

import com.bananarepublic.engine.GameState;
import com.bananarepublic.engine.TurnPhase;
import com.bananarepublic.exception.InvalidTradeException;
import com.bananarepublic.model.board.Intersection;
import com.bananarepublic.model.board.Path;
import com.bananarepublic.model.harbor.Harbor;
import com.bananarepublic.model.player.Player;
import com.bananarepublic.model.resource.ResourceInventory;
import com.bananarepublic.model.resource.ResourceType;

import java.util.Objects;

public class TradeService {
    private TradeOffer pendingOffer;

    public TradeResult submitDomesticTrade(GameState state, TradeOffer offer) {
        Objects.requireNonNull(state, "Game state cannot be null");
        validateTradePhase(state);
        validateOfferShape(Objects.requireNonNull(offer, "Trade offer cannot be null"));

        Player active = state.getCurrentPlayer();
        if (!active.getId().equals(offer.getProposerPlayerId())) {
            throw new InvalidTradeException("Only active player can initiate domestic trade");
        }

        Player responder = state.getPlayerById(offer.getResponderPlayerId());
        validateTradeParticipants(offer.getProposerPlayerId(), responder.getId());
        validateTradeResources(state, offer);
        pendingOffer = offer;
        return TradeResult.pending("Trade offer submitted", pendingOffer);
    }

    public TradeResult acceptDomesticTrade(GameState state, String responderPlayerId) {
        Objects.requireNonNull(state, "Game state cannot be null");
        validateTradePhase(state);
        TradeOffer offer = requirePendingOffer();

        if (!offer.getResponderPlayerId().equals(responderPlayerId)) {
            throw new InvalidTradeException("Only current responder can accept this offer");
        }

        validateTradeResources(state, offer);
        Player proposer = state.getPlayerById(offer.getProposerPlayerId());
        Player responder = state.getPlayerById(offer.getResponderPlayerId());

        executeTransfer(proposer, responder, offer);
        pendingOffer = null;
        return TradeResult.success("Trade accepted");
    }

    public TradeResult rejectDomesticTrade(GameState state, String responderPlayerId) {
        Objects.requireNonNull(state, "Game state cannot be null");
        validateTradePhase(state);
        TradeOffer offer = requirePendingOffer();
        if (!offer.getResponderPlayerId().equals(responderPlayerId)) {
            throw new InvalidTradeException("Only current responder can reject this offer");
        }
        pendingOffer = null;
        return TradeResult.rejected("Trade rejected");
    }

    public TradeResult counterDomesticTrade(GameState state, String responderPlayerId, TradeOffer counterOffer) {
        Objects.requireNonNull(state, "Game state cannot be null");
        validateTradePhase(state);
        TradeOffer current = requirePendingOffer();
        if (!current.getResponderPlayerId().equals(responderPlayerId)) {
            throw new InvalidTradeException("Only current responder can counter this offer");
        }

        validateOfferShape(Objects.requireNonNull(counterOffer, "Counter offer cannot be null"));
        if (!counterOffer.getProposerPlayerId().equals(current.getResponderPlayerId())
                || !counterOffer.getResponderPlayerId().equals(current.getProposerPlayerId())) {
            throw new InvalidTradeException("Counter offer must swap proposer and responder");
        }

        validateTradeParticipants(counterOffer.getProposerPlayerId(), counterOffer.getResponderPlayerId());
        validateCounterOfferResources(state, counterOffer);
        pendingOffer = counterOffer;
        return TradeResult.pending("Counter offer submitted", pendingOffer);
    }

    public TradeResult submitMaritimeTrade(GameState state, MaritimeTradeRequest request) {
        Objects.requireNonNull(state, "Game state cannot be null");
        validateTradePhase(state);
        Objects.requireNonNull(request, "Maritime trade request cannot be null");

        Player active = state.getCurrentPlayer();
        if (!active.getId().equals(request.getPlayerId())) {
            throw new InvalidTradeException("Only active player can perform maritime trade");
        }

        if (request.getOfferedType() == request.getRequestedType()) {
            throw new InvalidTradeException("Cannot trade same resource type in maritime trade");
        }

        int expectedRatio = resolveBestRatio(state, active, request.getOfferedType());
        if (request.getOfferedAmount() != expectedRatio) {
            throw new InvalidTradeException("Offered amount must match maritime ratio " + expectedRatio + ":1");
        }

        if (!active.hasResource(request.getOfferedType(), request.getOfferedAmount())) {
            throw new InvalidTradeException("Player does not have enough offered resources");
        }
        if (!state.getBank().hasResource(request.getRequestedType(), 1)) {
            throw new InvalidTradeException("Bank does not have requested resource");
        }

        active.removeResource(request.getOfferedType(), request.getOfferedAmount());
        state.getBank().returnResource(request.getOfferedType(), request.getOfferedAmount());
        state.getBank().take(request.getRequestedType(), 1);
        active.addResource(request.getRequestedType(), 1);
        return TradeResult.success("Maritime trade success");
    }

    public TradeOffer getPendingOffer() {
        return pendingOffer;
    }

    public void clearPendingOffer() {
        pendingOffer = null;
    }

    public int resolveBestRatio(GameState state, Player player, ResourceType offeredType) {
        int bestRatio = 4;
        for (Path path : player.getOwnedPipes().stream().map(pipe -> pipe.getLocation()).toList()) {
            Harbor harbor = path.getHarbor().orElse(null);
            if (harbor != null && harbor.isAccessibleBy(player) && harbor.canTradeWith(offeredType)) {
                bestRatio = Math.min(bestRatio, harbor.getRatio());
            }
        }
        for (Intersection intersection : player.getOwnedBuildings().stream().map(b -> b.getLocation()).toList()) {
            for (Path path : intersection.getConnectedPaths()) {
                Harbor harbor = path.getHarbor().orElse(null);
                if (harbor != null && harbor.isAccessibleBy(player) && harbor.canTradeWith(offeredType)) {
                    bestRatio = Math.min(bestRatio, harbor.getRatio());
                }
            }
        }
        return bestRatio;
    }

    private TradeOffer requirePendingOffer() {
        if (pendingOffer == null) {
            throw new InvalidTradeException("No pending domestic offer");
        }
        return pendingOffer;
    }

    private void validateTradePhase(GameState state) {
        if (state.getTurnState().getPhase() != TurnPhase.TRADE_BUILD) {
            throw new InvalidTradeException("Trade can only be performed in TRADE_BUILD phase");
        }
    }

    private void validateOfferShape(TradeOffer offer) {
        if (offer.getProposerPlayerId().equals(offer.getResponderPlayerId())) {
            throw new InvalidTradeException("Domestic trade must involve two different players");
        }
        if (offer.getOffered().isEmpty() || offer.getRequested().isEmpty()) {
            throw new InvalidTradeException("Domestic trade cannot be empty on either side");
        }
        if (sameResourceTypeOnly(offer.getOffered(), offer.getRequested())) {
            throw new InvalidTradeException("Cannot trade same resource type in domestic trade");
        }
    }

    private boolean sameResourceTypeOnly(ResourceInventory offered, ResourceInventory requested) {
        ResourceType offeredType = singleType(offered);
        ResourceType requestedType = singleType(requested);
        return offeredType != null && requestedType != null && offeredType == requestedType;
    }

    private ResourceType singleType(ResourceInventory inv) {
        ResourceType found = null;
        for (ResourceType type : ResourceType.values()) {
            if (inv.getAmount(type) > 0) {
                if (found != null) {
                    return null;
                }
                found = type;
            }
        }
        return found;
    }

    private void validateTradeParticipants(String proposerId, String responderId) {
        if (proposerId.equals(responderId)) {
            throw new InvalidTradeException("Domestic trade must involve two distinct players");
        }
    }

    private void validateTradeResources(GameState state, TradeOffer offer) {
        Player proposer = state.getPlayerById(offer.getProposerPlayerId());
        Player responder = state.getPlayerById(offer.getResponderPlayerId());
        if (!proposer.hasResources(offer.getOffered())) {
            throw new InvalidTradeException("Proposer lacks offered resources");
        }
        if (!responder.hasResources(offer.getRequested())) {
            throw new InvalidTradeException("Responder lacks requested resources");
        }
    }

    private void validateCounterOfferResources(GameState state, TradeOffer offer) {
        Player proposer = state.getPlayerById(offer.getProposerPlayerId());
        if (!proposer.hasResources(offer.getOffered())) {
            throw new InvalidTradeException("Proposer lacks offered resources");
        }
    }

    private void executeTransfer(Player proposer, Player responder, TradeOffer offer) {
        proposer.removeResources(offer.getOffered());
        responder.addResources(offer.getOffered());

        responder.removeResources(offer.getRequested());
        proposer.addResources(offer.getRequested());
    }
}
