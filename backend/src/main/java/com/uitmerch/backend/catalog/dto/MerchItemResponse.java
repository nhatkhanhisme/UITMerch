package com.uitmerch.backend.catalog.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for merch item details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Merchandise item response")
public class MerchItemResponse {

    @JsonProperty("id")
    @Schema(description = "Merchandise UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    @JsonProperty("organization")
    @Schema(description = "Owning organization")
    private MerchOrganizationInfo organization;

    @JsonProperty("name")
    @Schema(description = "Merchandise name", example = "UIT Hoodie 2026")
    private String name;

    @JsonProperty("meaningText")
    @Schema(description = "Description or meaning text", example = "Official UIT merchandise for campus events")
    private String meaningText;

    @JsonProperty("price")
    @Schema(description = "Sale price in VND", example = "150000.00")
    private BigDecimal price;

    @JsonProperty("stockQuantity")
    @Schema(description = "Current stock quantity", example = "100")
    private Integer stockQuantity;

    @JsonProperty("isPreorder")
    @Schema(description = "Whether the item is preorder", example = "false")
    private Boolean isPreorder;

    @JsonProperty("isActive")
    @Schema(description = "Whether the item is visible to public users", example = "true")
    private Boolean isActive;

    @JsonProperty("imageUrl")
    @Schema(description = "Public image URL", example = "https://example.supabase.co/storage/v1/object/public/merch/hoodie.png")
    private String imageUrl;

    @JsonProperty("createdAt")
    @Schema(description = "Creation timestamp", example = "2026-05-06T10:00:00Z")
    private Instant createdAt;

    @JsonProperty("updatedAt")
    @Schema(description = "Last update timestamp", example = "2026-05-06T12:00:00Z")
    private Instant updatedAt;
}