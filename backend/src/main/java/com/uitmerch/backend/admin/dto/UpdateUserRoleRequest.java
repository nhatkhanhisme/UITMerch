package com.uitmerch.backend.admin.dto;

import com.uitmerch.backend.common.model.UserRole;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateUserRoleRequest {

    @NotNull(message = "Role is required")
    private UserRole role;
}
