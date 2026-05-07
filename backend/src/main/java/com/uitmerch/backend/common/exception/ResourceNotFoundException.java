package com.uitmerch.backend.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a requested resource is not found.
 * Returns HTTP 404.
 */
public class ResourceNotFoundException extends AppException {
    
    public ResourceNotFoundException(String resourceName, String identifier) {
        super(
            String.format("%s not found with identifier: %s", resourceName, identifier),
            HttpStatus.NOT_FOUND,
            "RESOURCE_NOT_FOUND"
        );
    }
    
    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
    }
}
