package com.premisave.property.exception;

/**
 * The wallet-service payment could not be completed or confirmed (outage,
 * timeout, misconfiguration) or a payment was debited but not yet recorded.
 * Mapped to HTTP 503 by GlobalExceptionHandler.
 */
public class WalletServiceException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public WalletServiceException(String message) {
        super(message);
    }

    public WalletServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}