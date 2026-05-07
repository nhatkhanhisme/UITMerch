package com.uitmerch.backend.common.exception;

import org.springframework.http.HttpStatus;

public class InvalidOtpException extends AppException {

    public InvalidOtpException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "AUTH_004");
    }
}
