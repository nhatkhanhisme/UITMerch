package com.uitmerch.backend.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Email verification using OTP")
public class VerifyEmailRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Schema(example = "student@uit.edu.vn")
    private String email;

    @NotBlank(message = "OTP code is required")
    @Schema(example = "482910", description = "6-digit OTP sent to registered email")
    private String otpCode;
}
