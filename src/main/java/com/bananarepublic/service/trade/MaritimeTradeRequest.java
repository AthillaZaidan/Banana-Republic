package com.bananarepublic.service.trade;

import com.bananarepublic.model.resource.ResourceType;

import java.util.Objects;

public final class MaritimeTradeRequest {
    private final String playerId;
    private final ResourceType offeredType;
    private final int offeredAmount;
    private final ResourceType requestedType;

    public MaritimeTradeRequest(String playerId, ResourceType offeredType, int offeredAmount, ResourceType requestedType) {
        if (playerId == null || playerId.isBlank()) {
            throw new IllegalArgumentException("Player id cannot be empty");
        }
        if (offeredAmount <= 0) {
            throw new IllegalArgumentException("Offered amount must be positive");
        }
        this.playerId = playerId;
        this.offeredType = Objects.requireNonNull(offeredType, "Offered type cannot be null");
        this.offeredAmount = offeredAmount;
        this.requestedType = Objects.requireNonNull(requestedType, "Requested type cannot be null");
    }

    public String getPlayerId() {
        return playerId;
    }

    public ResourceType getOfferedType() {
        return offeredType;
    }

    public int getOfferedAmount() {
        return offeredAmount;
    }

    public ResourceType getRequestedType() {
        return requestedType;
    }
}

