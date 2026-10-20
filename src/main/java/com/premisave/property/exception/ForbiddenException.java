package com.premisave.property.exception;

// The caller is signed in but is not allowed to touch this record (HTTP 403).
// UnauthorizedException (401) stays for "not signed in".
@SuppressWarnings("serial")
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
