package com.premisave.property.exception;

@SuppressWarnings("serial")
public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}