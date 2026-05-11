package com.uitmerch.backend.admin.dto;

import com.uitmerch.backend.common.model.OrganizationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateOrganizationStatusRequest {

    @NotNull(message = "Status is required")
    private OrganizationStatus status;
}
