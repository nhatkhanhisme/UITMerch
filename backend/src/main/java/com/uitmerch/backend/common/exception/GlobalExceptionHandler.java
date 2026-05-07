package com.uitmerch.backend.common.exception;

import com.uitmerch.backend.common.model.ApiResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Global exception handler for all REST endpoints.
 * Converts exceptions to standard API response envelope with error codes.
 * 
 * Error Code Format: SYS_XXX or DOMAIN_XXX
 * - SYS_001: Validation error (MethodArgumentNotValidException)
 * - SYS_002: Bad request
 * - SYS_003: Resource not found
 * - SYS_004: Unauthorized
 * - SYS_005: Internal server error
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * Handle validation errors from @Valid annotations.
     * Error Code: SYS_001
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<ErrorDetail>> handleMethodArgumentNotValid(
        MethodArgumentNotValidException ex,
        WebRequest request
    ) {
        String traceId = getOrCreateTraceId();
        logger.warn("Validation error: {}", ex.getMessage());
        
        List<FieldError> fieldErrors = new ArrayList<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
            fieldErrors.add(FieldError.builder()
                .field(error.getField())
                .message(error.getDefaultMessage())
                .rejectedValue(error.getRejectedValue() != null ? error.getRejectedValue().toString() : null)
                .build())
        );
        
        ErrorDetail errorDetail = ErrorDetail.builder()
            .errorCode("SYS_001")
            .message("Request validation failed")
            .timestamp(Instant.now().toString())
            .fieldErrors(fieldErrors)
            .build();
        
        ApiResponse<ErrorDetail> response = ApiResponse.<ErrorDetail>builder()
            .data(errorDetail)
            .traceId(traceId)
            .build();
        
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<ErrorDetail>> handleUserAlreadyExists(
        UserAlreadyExistsException ex,
        WebRequest request
    ) {
        String traceId = getOrCreateTraceId();
        logger.warn("User already exists: {}", ex.getMessage());

        ErrorDetail errorDetail = ErrorDetail.builder()
            .errorCode(ex.getErrorCode())
            .message(ex.getMessage())
            .timestamp(Instant.now().toString())
            .build();

        ApiResponse<ErrorDetail> response = ApiResponse.<ErrorDetail>builder()
            .data(errorDetail)
            .traceId(traceId)
            .build();

        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(InvalidOtpException.class)
    public ResponseEntity<ApiResponse<ErrorDetail>> handleInvalidOtp(
        InvalidOtpException ex,
        WebRequest request
    ) {
        String traceId = getOrCreateTraceId();
        logger.warn("Invalid OTP: {}", ex.getMessage());

        ErrorDetail errorDetail = ErrorDetail.builder()
            .errorCode(ex.getErrorCode())
            .message(ex.getMessage())
            .timestamp(Instant.now().toString())
            .build();

        ApiResponse<ErrorDetail> response = ApiResponse.<ErrorDetail>builder()
            .data(errorDetail)
            .traceId(traceId)
            .build();

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UnverifiedEmailException.class)
    public ResponseEntity<ApiResponse<ErrorDetail>> handleUnverifiedEmail(
        UnverifiedEmailException ex,
        WebRequest request
    ) {
        String traceId = getOrCreateTraceId();
        logger.warn("Unverified email login attempt: {}", ex.getMessage());

        ErrorDetail errorDetail = ErrorDetail.builder()
            .errorCode("AUTH_005")
            .message(ex.getMessage())
            .timestamp(Instant.now().toString())
            .build();

        ApiResponse<ErrorDetail> response = ApiResponse.<ErrorDetail>builder()
            .data(errorDetail)
            .traceId(traceId)
            .build();

        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }
    
    /**
     * Handle 404 Not Found errors.
     * Error Code: SYS_003
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<ErrorDetail>> handleNoHandlerFound(
        NoHandlerFoundException ex,
        WebRequest request
    ) {
        String traceId = getOrCreateTraceId();
        logger.warn("Resource not found: {} {}", ex.getHttpMethod(), ex.getRequestURL());
        
        ErrorDetail errorDetail = ErrorDetail.builder()
            .errorCode("SYS_003")
            .message("Endpoint not found: " + ex.getRequestURL())
            .timestamp(Instant.now().toString())
            .build();
        
        ApiResponse<ErrorDetail> response = ApiResponse.<ErrorDetail>builder()
            .data(errorDetail)
            .traceId(traceId)
            .build();
        
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }
    
    /**
     * Handle domain-specific exceptions.
     * Preserves error code from AppException subclasses.
     */
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<ErrorDetail>> handleAppException(
        AppException ex,
        WebRequest request
    ) {
        String traceId = getOrCreateTraceId();
        logger.warn("Application exception [{}]: {}", ex.getErrorCode(), ex.getMessage());
        
        ErrorDetail errorDetail = ErrorDetail.builder()
            .errorCode(ex.getErrorCode())
            .message(ex.getMessage())
            .timestamp(Instant.now().toString())
            .build();
        
        ApiResponse<ErrorDetail> response = ApiResponse.<ErrorDetail>builder()
            .data(errorDetail)
            .traceId(traceId)
            .build();
        
        return new ResponseEntity<>(response, ex.getHttpStatus());
    }
    
    /**
     * Handle all other exceptions.
     * Error Code: SYS_005
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<ErrorDetail>> handleGenericException(
        Exception ex,
        WebRequest request
    ) {
        String traceId = getOrCreateTraceId();
        logger.error("Unhandled exception: {}", ex.getMessage(), ex);
        
        ErrorDetail errorDetail = ErrorDetail.builder()
            .errorCode("SYS_005")
            .message("An unexpected error occurred")
            .timestamp(Instant.now().toString())
            .build();
        
        ApiResponse<ErrorDetail> response = ApiResponse.<ErrorDetail>builder()
            .data(errorDetail)
            .traceId(traceId)
            .build();
        
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    /**
     * Get or create trace ID from MDC.
     */
    private String getOrCreateTraceId() {
        String traceId = MDC.get("traceId");
        if (traceId == null) {
            traceId = UUID.randomUUID().toString();
            MDC.put("traceId", traceId);
        }
        return traceId;
    }
    
    /**
     * Error response detail with optional field validation errors.
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorDetail {
        @JsonProperty("errorCode")
        private String errorCode;
        
        @JsonProperty("message")
        private String message;
        
        @JsonProperty("timestamp")
        private String timestamp;
        
        @JsonProperty("fieldErrors")
        private List<FieldError> fieldErrors;
    }
    
    /**
     * Detailed validation error for a single field.
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FieldError {
        @JsonProperty("field")
        private String field;
        
        @JsonProperty("message")
        private String message;
        
        @JsonProperty("rejectedValue")
        private String rejectedValue;
    }
}
