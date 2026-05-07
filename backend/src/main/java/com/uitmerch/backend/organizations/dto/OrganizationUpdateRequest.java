package com.uitmerch.backend.organizations.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating an organization profile.
 * FR04: Organizer can update organization profile, bio, and logo.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Organization update request")
public class OrganizationUpdateRequest {

    @JsonProperty("name")
    @Size(min = 2, max = 180, message = "Organization name must be between 2 and 180 characters")
    @Schema(description = "Organization name", example = "UITMerch Club")
    private String name;

    @JsonProperty("description")
    @Size(max = 5000, message = "Organization description must be at most 5000 characters")
    @Schema(description = "Organization description or bio", example = "Official merchandise team for UIT events")
    private String description;

    @JsonProperty("logoUrl")
    @Size(max = 2048, message = "Logo URL must be at most 2048 characters")
    @Schema(description = "Public logo URL from Supabase Storage", example = "https://example.supabase.co/storage/v1/object/public/org-logos/logo.png")
    private String logoUrl;
}