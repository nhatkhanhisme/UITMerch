package com.uitmerch.backend.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uitmerch.backend.common.exception.GlobalExceptionHandler;
import com.uitmerch.backend.common.model.ApiResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

/**
 * JWT Authentication Entry Point.
 * Handles authentication errors and returns standard API response envelope.
 * Triggered when unauthenticated user tries to access protected resource.
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationEntryPoint.class);
    
    @Override
    public void commence(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException authException
    ) throws IOException, ServletException {
        logger.error("Responding with unauthorized error: {}", authException.getMessage());
        
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        
        String traceId = UUID.randomUUID().toString();
        
        final GlobalExceptionHandler.ErrorDetail errorDetail = GlobalExceptionHandler.ErrorDetail.builder()
            .errorCode("AUTHENTICATION_FAILED")
            .message("Unauthorized access - please provide valid JWT token")
            .timestamp(Instant.now().toString())
            .build();
        
        final ApiResponse<GlobalExceptionHandler.ErrorDetail> body = ApiResponse.<GlobalExceptionHandler.ErrorDetail>builder()
            .data(errorDetail)
            .traceId(traceId)
            .build();
        
        final ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(response.getOutputStream(), body);
    }
}
