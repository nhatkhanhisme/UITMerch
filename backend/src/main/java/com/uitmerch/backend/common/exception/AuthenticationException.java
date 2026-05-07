package com.uitmerch.backend.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when authentication fails (invalid credentials).
 * Returns HTTP 401 Unauthorized.
 */
public class AuthenticationException extends AppException {
    
    public AuthenticationException(String message) {
        super(message, HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED");
    }
    
    public AuthenticationException(String message, String errorCode) {
        super(message, HttpStatus.UNAUTHORIZED, errorCode);
    }
}
