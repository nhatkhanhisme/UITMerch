package com.uitmerch.backend.organizations.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.uitmerch.backend.common.model.OrganizationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for organization details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Organization response")
public class OrganizationResponse {

    @JsonProperty("id")
    @Schema(description = "Organization ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    @JsonProperty("ownerUserId")
    @Schema(description = "Owner user ID", example = "550e8400-e29b-41d4-a716-446655440111")
    private UUID ownerUserId;

    @JsonProperty("name")
    @Schema(description = "Organization name", example = "UITMerch Club")
    private String name;

    @JsonProperty("description")
    @Schema(description = "Organization bio/description", example = "Official merchandise team for UIT events")
    private String description;

    @JsonProperty("logoUrl")
    @Schema(description = "Public logo URL", example = "https://example.supabase.co/storage/v1/object/public/org-logos/logo.png")
    private String logoUrl;

    @JsonProperty("status")
    @Schema(description = "Organization status", example = "ACTIVE")
    private OrganizationStatus status;

    @JsonProperty("createdAt")
    @Schema(description = "Creation timestamp", example = "2026-05-06T10:00:00Z")
    private Instant createdAt;

    @JsonProperty("updatedAt")
    @Schema(description = "Last update timestamp", example = "2026-05-06T12:00:00Z")
    private Instant updatedAt;
}