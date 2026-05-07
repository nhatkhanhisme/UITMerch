package com.uitmerch.backend.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when file upload to Supabase Storage fails.
 * Returns HTTP 500 Internal Server Error.
 * 
 * NFR03: Supabase Storage used for images; Base64/BLOB persistence in DB is strictly prohibited.
 */
public class StorageException extends AppException {
    
    public StorageException(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE_ERROR");
    }
    
    public StorageException(String message, Throwable cause) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE_ERROR", cause);
    }
    
    public StorageException(String message, String errorCode, Throwable cause) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, errorCode, cause);
    }
}
