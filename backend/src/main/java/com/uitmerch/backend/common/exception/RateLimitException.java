package com.uitmerch.backend.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class RateLimitException extends AppException {
    private final long retryAfterSeconds;
    public RateLimitException(String message, long retryAfterSeconds) {
        super(message, HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED");
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
