package com.uitmerch.backend.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Customer registration request")
public class RegisterRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Schema(example = "student@uit.edu.vn")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters and include uppercase, lowercase, and a number")
    @Schema(example = "Password1", description = "Min 8 chars, must include uppercase, lowercase, and digit")
    private String password;

    @NotBlank(message = "Full name is required")
    @Schema(example = "Nguyen Van A")
    private String fullName;

    @Schema(example = "0901234567")
    private String phone;

    @Schema(example = "123 Nguyen Van Cu, Q5, TP.HCM")
    private String address;
}
