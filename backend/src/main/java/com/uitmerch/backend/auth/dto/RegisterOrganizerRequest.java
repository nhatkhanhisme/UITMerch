package com.uitmerch.backend.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Organizer registration request")
public class RegisterOrganizerRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Schema(example = "club@uit.edu.vn")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters and include uppercase, lowercase, and a number")
    @Schema(example = "Password1", description = "Min 8 chars, must include uppercase, lowercase, and digit")
    private String password;

    @NotBlank(message = "Full name is required")
    @Schema(example = "Tran Thi B")
    private String fullName;

    @Schema(example = "0907654321")
    private String phone;

    @Schema(example = "UIT Campus, Thu Duc, TP.HCM")
    private String address;
}
