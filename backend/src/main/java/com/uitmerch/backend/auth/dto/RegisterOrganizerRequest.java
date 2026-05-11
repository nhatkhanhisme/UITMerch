package com.uitmerch.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterOrganizerRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters and include uppercase, lowercase, and a number")
    private String password;

    @NotBlank(message = "Full name is required")
    private String fullName;

    private String phone;

    private String address;
}
