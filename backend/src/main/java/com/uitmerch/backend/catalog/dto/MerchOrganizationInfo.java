package com.uitmerch.backend.catalog.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Abbreviated organization data included in merch responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Organization info in merchandise response")
public class MerchOrganizationInfo {

    @JsonProperty("id")
    @Schema(description = "Organization UUID", example = "550e8400-e29b-41d4-a716-446655440111")
    private UUID id;

    @JsonProperty("name")
    @Schema(description = "Organization name", example = "UITMerch Club")
    private String name;

    @JsonProperty("logoUrl")
    @Schema(description = "Organization logo URL", example = "https://example.supabase.co/storage/v1/object/public/org-logos/logo.png")
    private String logoUrl;
}