package com.uitmerch.backend.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.uitmerch.backend.common.model.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating user role.
 * Admin-only endpoint for promoting/demoting user roles.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRoleRequest {
    
    @JsonProperty("role")
    @Schema(example = "ORGANIZER", description = "New role for the user (CUSTOMER, ORGANIZER, ADMIN)")
    @NotNull(message = "Role is required")
    private UserRole role;
}
