package com.bananarepublic.service.trade;

import com.bananarepublic.model.resource.ResourceInventory;

import java.util.Objects;

public final class TradeOffer {
    private final String proposerPlayerId;
    private final String responderPlayerId;
    private final ResourceInventory offered;
    private final ResourceInventory requested;

    public TradeOffer(
            String proposerPlayerId,
            String responderPlayerId,
            ResourceInventory offered,
            ResourceInventory requested
    ) {
        if (proposerPlayerId == null || proposerPlayerId.isBlank()) {
            throw new IllegalArgumentException("Proposer player id cannot be empty");
        }
        if (responderPlayerId == null || responderPlayerId.isBlank()) {
            throw new IllegalArgumentException("Responder player id cannot be empty");
        }
        this.proposerPlayerId = proposerPlayerId;
        this.responderPlayerId = responderPlayerId;
        this.offered = Objects.requireNonNull(offered, "Offered inventory cannot be null").copy();
        this.requested = Objects.requireNonNull(requested, "Requested inventory cannot be null").copy();
    }

    public String getProposerPlayerId() {
        return proposerPlayerId;
    }

    public String getResponderPlayerId() {
        return responderPlayerId;
    }

    public ResourceInventory getOffered() {
        return offered.copy();
    }

    public ResourceInventory getRequested() {
        return requested.copy();
    }
}

