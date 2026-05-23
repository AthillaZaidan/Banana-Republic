package com.bananarepublic.exception;

public class InvalidMoveException extends GameRuleViolationException {
    public InvalidMoveException(String message) {
        super(message);
    }
}
