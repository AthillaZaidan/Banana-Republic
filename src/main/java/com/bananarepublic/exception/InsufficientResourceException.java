package com.bananarepublic.exception;

public class InsufficientResourceException extends GameRuleViolationException {
    public InsufficientResourceException(String message) {
        super(message);
    }
}
