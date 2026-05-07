package com.uitmerch.backend.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when user lacks permission to access resource.
 * Returns HTTP 403 Forbidden.
 */
public class ForbiddenException extends AppException {
    
    public ForbiddenException(String message) {
        super(message, HttpStatus.FORBIDDEN, "ACCESS_DENIED");
    }
    
    public ForbiddenException(String message, String errorCode) {
        super(message, HttpStatus.FORBIDDEN, errorCode);
    }
}
