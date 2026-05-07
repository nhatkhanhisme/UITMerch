package com.uitmerch.backend.common.exception;

import org.springframework.http.HttpStatus;

public class UserAlreadyExistsException extends AppException {

    public UserAlreadyExistsException(String message) {
        super(message, HttpStatus.CONFLICT, "AUTH_001");
    }
}
