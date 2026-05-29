package com.bananarepublic.exception;

public class SaveLoadException extends RuntimeException {
    public SaveLoadException(String message) {
        super(message);
    }

    public SaveLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}