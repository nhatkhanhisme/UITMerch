package com.uitmerch.backend.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for user login.
 * FR02: Session management with JWT accessToken and refreshToken.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    
    @JsonProperty("email")
    @Schema(example = "test-customer@uit.edu.vn")
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;
    
    @JsonProperty("password")
    @Schema(example = "Password123!")
    @NotBlank(message = "Password is required")
    private String password;
}
