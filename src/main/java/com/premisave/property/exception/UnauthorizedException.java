package com.premisave.property.exception;

@SuppressWarnings("serial")
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}