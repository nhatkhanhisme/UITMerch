package com.uitmerch.backend.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.uitmerch.backend.common.model.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User summary returned after authentication.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthUserSummary {

    @JsonProperty("userId")
    private String userId;

    @JsonProperty("email")
    private String email;

    @JsonProperty("fullName")
    private String fullName;

    @JsonProperty("role")
    private UserRole role;

    @JsonProperty("isEmailVerified")
    private boolean isEmailVerified;
}
