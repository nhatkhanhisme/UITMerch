package com.uitmerch.backend.organization.dto;

import lombok.Data;

@Data
public class UpdateOrganizationRequest {

    private String name;
    private String description;
    private String logoUrl;
    private String coverUrl;
}
