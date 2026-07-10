package com.premisave.property.exception;

@SuppressWarnings("serial")
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}