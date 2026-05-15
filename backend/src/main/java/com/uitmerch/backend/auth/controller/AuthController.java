package com.uitmerch.backend.auth.controller;

import com.uitmerch.backend.auth.dto.*;
import com.uitmerch.backend.common.exception.ValidationException;
import com.uitmerch.backend.common.model.ApiResponse;
import com.uitmerch.backend.auth.service.AuthService;
import com.uitmerch.backend.common.service.RateLimiterService;
import com.uitmerch.backend.common.util.IpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Registration, email verification, and login")
public class AuthController {

    private final AuthService authService;
    private final RateLimiterService rateLimiterService;
    private final IpUtil ipUtil;

    private static final int LOGIN_MAX_ATTEMPTS    = 10;
    private static final Duration LOGIN_WINDOW     = Duration.ofMinutes(15);
    private static final int REGISTER_MAX_ATTEMPTS = 5;
    private static final Duration REGISTER_WINDOW  = Duration.ofHours(1);

    @PostMapping("/register")
    @Operation(summary = "Register customer account")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Registration successful — OTP sent to email"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed — see data for field errors"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Email already registered"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<ApiResponse<Void>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest
    ) {
        if (!rateLimiterService.isAllowed("register:" + ipUtil.extractClientIp(httpRequest), REGISTER_MAX_ATTEMPTS, REGISTER_WINDOW)) {
            throw new ValidationException("Too many registration attempts. Please try again later.");
        }
        authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration successful. Please check your email for the OTP.", null));
    }

    @PostMapping("/register/organizer")
    @Operation(summary = "Register organizer account")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Organizer registration successful — OTP sent to email"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed — see data for field errors"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Email already registered"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<ApiResponse<Void>> registerOrganizer(
            @Valid @RequestBody RegisterOrganizerRequest request,
            HttpServletRequest httpRequest
    ) {
        if (!rateLimiterService.isAllowed("register:" + ipUtil.extractClientIp(httpRequest), REGISTER_MAX_ATTEMPTS, REGISTER_WINDOW)) {
            throw new ValidationException("Too many registration attempts. Please try again later.");
        }
        authService.registerOrganizer(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Organizer registration successful. Please check your email for the OTP.", null));
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verify email with OTP")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Email verified successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed or OTP is invalid/expired"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<ApiResponse<Void>> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request
    ) {
        authService.verifyEmail(request);
        return ResponseEntity.ok(ApiResponse.success("Email verified successfully.", null));
    }

    @PostMapping("/login")
    @Operation(summary = "Login and receive JWT tokens")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login successful — JWT tokens returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed — see data for field errors"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid email or password"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Email not yet verified"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        if (!rateLimiterService.isAllowed("login:" + ipUtil.extractClientIp(httpRequest), LOGIN_MAX_ATTEMPTS, LOGIN_WINDOW)) {
            throw new ValidationException("Too many login attempts. Please try again in 15 minutes.");
        }
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful.", response));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Exchange a valid refresh token for a new access token and rotated refresh token.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tokens refreshed successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed — refresh token missing"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Refresh token is invalid or expired"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        AuthResponse response = authService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success("Tokens refreshed successfully.", response));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request a password-reset OTP", description = "Sends a reset OTP to the email if the account exists and is active. Always returns 200 to prevent user enumeration.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "If the account exists, an OTP has been sent"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed")
    })
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpRequest
    ) {
        if (!rateLimiterService.isAllowed("pwd-reset:" + ipUtil.extractClientIp(httpRequest), REGISTER_MAX_ATTEMPTS, REGISTER_WINDOW)) {
            throw new ValidationException("Too many password-reset attempts. Please try again later.");
        }
        authService.forgotPassword(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("If the account exists, a reset code has been sent.", null));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using OTP")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Password reset successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid or expired OTP, or weak password")
    })
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset successfully. You can now log in.", null));
    }

    @PostMapping("/logout")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Logout and invalidate the current JWT")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Logged out successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid token")
    })
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        String token = (authHeader != null && authHeader.startsWith("Bearer "))
                ? authHeader.substring(7) : null;
        authService.logout(token);
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully.", null));
    }
}
