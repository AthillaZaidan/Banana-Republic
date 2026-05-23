package com.bananarepublic.exception;

public class GameRuleViolationException extends RuntimeException {
    public GameRuleViolationException(String message) {
        super(message);
    }
}
