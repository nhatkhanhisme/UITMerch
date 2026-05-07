package com.uitmerch.backend.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.uitmerch.backend.common.model.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO after updating user role.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateResponse {

    @JsonProperty("userId")
    private String userId;

    @JsonProperty("email")
    private String email;

    @JsonProperty("fullName")
    private String fullName;

    @JsonProperty("role")
    private UserRole role;

    @JsonProperty("message")
    private String message;
}
