package com.uitmerch.backend.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for user registration.
 * FR01: Flexible registration with email, password, full name.
 * BR01: Email is globally unique.
 * BR02: Password >= 8 chars, uppercase, lowercase, number.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    
    @JsonProperty("email")
    @Schema(example = "test@uit.edu.vn")
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;
    
    @JsonProperty("password")
    @Schema(example = "Password123!")
    @NotBlank(message = "Password is required")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$",
        message = "Password must be at least 8 characters and include uppercase, lowercase, and a number"
    )
    private String password;
    
    @JsonProperty("fullName")
    @Schema(example = "Test User")
    @NotBlank(message = "Full name is required")
    @Size(max = 150, message = "Full name must be 150 characters or less")
    private String fullName;
}
