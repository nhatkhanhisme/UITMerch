package com.uitmerch.backend.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base application exception class.
 * All domain-specific exceptions should extend this.
 */
@Getter
public class AppException extends RuntimeException {
    
    private final HttpStatus httpStatus;
    private final String errorCode;
    
    public AppException(String message, HttpStatus httpStatus, String errorCode) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }
    
    public AppException(String message, HttpStatus httpStatus, String errorCode, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }
}
