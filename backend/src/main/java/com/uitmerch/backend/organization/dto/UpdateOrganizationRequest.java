package com.uitmerch.backend.organization.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

@Data
public class UpdateOrganizationRequest {

    @Size(max = 200, message = "Organization name must not exceed 200 characters")
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @URL(message = "Logo URL must be a valid URL")
    @Size(max = 500, message = "Logo URL must not exceed 500 characters")
    private String logoUrl;

    @URL(message = "Cover URL must be a valid URL")
    @Size(max = 500, message = "Cover URL must not exceed 500 characters")
    private String coverUrl;
}
