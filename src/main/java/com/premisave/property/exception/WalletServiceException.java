package com.premisave.property.exception;

/**
 * A wallet payment could not be confirmed or recorded, or the payment service failed
 * (as opposed to being offline — see ServiceOfflineException). Mapped to HTTP 503 by
 * GlobalExceptionHandler; the optional code lets the frontend tell the cases apart:
 * PAYMENT_UNCONFIRMED, PAYMENT_NOT_RECORDED, PAYMENT_SERVICE_ERROR.
 */
public class WalletServiceException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final String code;

    public WalletServiceException(String message) {
        this(message, null, null);
    }

    public WalletServiceException(String message, String code) {
        this(message, code, null);
    }

    public WalletServiceException(String message, Throwable cause) {
        this(message, null, cause);
    }

    public WalletServiceException(String message, String code, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}