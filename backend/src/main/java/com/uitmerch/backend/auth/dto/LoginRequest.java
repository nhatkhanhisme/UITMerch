package com.uitmerch.backend.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Login credentials")
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Schema(example = "student@uit.edu.vn")
    private String email;

    @NotBlank(message = "Password is required")
    @Schema(example = "Password1")
    private String password;
}
