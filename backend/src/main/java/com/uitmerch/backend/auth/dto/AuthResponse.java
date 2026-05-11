package com.uitmerch.backend.auth.dto;

import com.uitmerch.backend.common.model.UserRole;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class AuthResponse {

    private String token;
    private String tokenType;
    private String refreshToken;
    private UUID userId;
    private String email;
    private String fullName;
    private UserRole role;
    private boolean isVerified;
}
