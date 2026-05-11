package com.uitmerch.backend.organization.dto;

import com.uitmerch.backend.common.model.OrganizationStatus;
import com.uitmerch.backend.organization.entity.Organization;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class OrganizationResponse {

    private UUID id;
    private UUID ownerId;
    private String name;
    private String description;
    private String logoUrl;
    private String coverUrl;
    private OrganizationStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static OrganizationResponse from(Organization org) {
        return OrganizationResponse.builder()
            .id(org.getId())
            .ownerId(org.getOwnerId())
            .name(org.getName())
            .description(org.getDescription())
            .logoUrl(org.getLogoUrl())
            .coverUrl(org.getCoverUrl())
            .status(org.getStatus())
            .createdAt(org.getCreatedAt())
            .updatedAt(org.getUpdatedAt())
            .build();
    }
}
