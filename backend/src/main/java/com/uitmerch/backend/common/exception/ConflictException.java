package com.uitmerch.backend.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a resource already exists (e.g., duplicate email).
 * Returns HTTP 409 Conflict.
 */
public class ConflictException extends AppException {
    
    public ConflictException(String message) {
        super(message, HttpStatus.CONFLICT, "CONFLICT");
    }
    
    public ConflictException(String message, String errorCode) {
        super(message, HttpStatus.CONFLICT, errorCode);
    }
}
