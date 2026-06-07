package com.bananarepublic.service.trade;

public final class TradeResult {
    private final boolean success;
    private final String message;
    private final TradeOffer pendingOffer;

    private TradeResult(boolean success, String message, TradeOffer pendingOffer) {
        this.success = success;
        this.message = message;
        this.pendingOffer = pendingOffer;
    }

    public static TradeResult success(String message) {
        return new TradeResult(true, message, null);
    }

    public static TradeResult pending(String message, TradeOffer offer) {
        return new TradeResult(false, message, offer);
    }

    public static TradeResult rejected(String message) {
        return new TradeResult(false, message, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public TradeOffer getPendingOffer() {
        return pendingOffer;
    }
}

