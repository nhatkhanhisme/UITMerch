package com.uitmerch.backend.common.exception;

import org.springframework.http.HttpStatus;

public class UnverifiedEmailException extends AppException {

    public UnverifiedEmailException(String message) {
        super(message, HttpStatus.FORBIDDEN, "AUTH_005");
    }
}
