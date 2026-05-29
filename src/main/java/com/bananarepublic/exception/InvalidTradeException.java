package com.bananarepublic.exception;

public class InvalidTradeException extends GameRuleViolationException {
    public InvalidTradeException(String message) {
        super(message);
    }
}

