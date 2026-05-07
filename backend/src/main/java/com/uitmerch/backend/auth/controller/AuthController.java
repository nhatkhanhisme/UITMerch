package com.uitmerch.backend.auth.controller;

import com.uitmerch.backend.auth.dto.AuthResponse;
import com.uitmerch.backend.auth.dto.LoginRequest;
import com.uitmerch.backend.auth.dto.MessageResponse;
import com.uitmerch.backend.auth.dto.RegisterRequest;
import com.uitmerch.backend.auth.dto.VerifyEmailRequest;
import com.uitmerch.backend.auth.service.AuthService;
import com.uitmerch.backend.common.model.ApiResponse;
import com.uitmerch.backend.common.util.TraceIdUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "1. Authentication", description = "Registration, email verification (OTP), and login")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(
        summary = "Register a new user",
        description = "Registers user and generates OTP for email verification.",
        security = {} // No security required for this public endpoint
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "User registered successfully",
            content = @Content(schema = @Schema(implementation = MessageResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Email already exists")
    })
    public ResponseEntity<ApiResponse<MessageResponse>> register(@Valid @RequestBody RegisterRequest request) {
        MessageResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(response, TraceIdUtil.getOrCreateTraceId()));
    }

    @PostMapping("/register/organizer")
    @Operation(
        summary = "Register a new organizer",
        description = "Registers organizer with ORGANIZER role and generates OTP for email verification.",
        security = {} // No security required for this public endpoint
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "Organizer registered successfully",
            content = @Content(schema = @Schema(implementation = MessageResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Email already exists")
    })
    public ResponseEntity<ApiResponse<MessageResponse>> registerOrganizer(@Valid @RequestBody RegisterRequest request) {
        MessageResponse response = authService.registerOrganizer(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(response, TraceIdUtil.getOrCreateTraceId()));
    }

    @PostMapping("/verify-email")
    @Operation(
        summary = "Verify email OTP",
        description = "Verifies user email with the OTP code.",
        security = {} // No security required for this public endpoint
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Email verified successfully",
            content = @Content(schema = @Schema(implementation = MessageResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid or expired OTP")
    })
    public ResponseEntity<ApiResponse<MessageResponse>> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        MessageResponse response = authService.verifyEmail(request);
        return ResponseEntity.ok(ApiResponse.success(response, TraceIdUtil.getOrCreateTraceId()));
    }

    @PostMapping("/login")
    @Operation(
        summary = "Login",
        description = "Authenticates user and returns JWT tokens.",
        security = {} // No security required for this public endpoint
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Login successful",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid credentials"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Email not verified")
    })
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response, TraceIdUtil.getOrCreateTraceId()));
    }
}
