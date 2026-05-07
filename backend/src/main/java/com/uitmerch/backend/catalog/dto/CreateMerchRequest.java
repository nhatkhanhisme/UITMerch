package com.uitmerch.backend.catalog.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for organizer merch creation.
 * BR07: Pre-order items may be created with zero stock, while ready-stock items require positive stock.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Create merchandise request")
public class CreateMerchRequest {

    @JsonProperty("name")
    @NotBlank(message = "Merch name is required")
    @Size(min = 1, max = 180, message = "Merch name must be between 1 and 180 characters")
    @Schema(description = "Merchandise name", example = "UIT Hoodie 2026")
    private String name;

    @JsonProperty("meaningText")
    @Size(max = 5000, message = "Meaning text must be at most 5000 characters")
    @Schema(description = "Description or story of the merchandise", example = "Official UIT merchandise for campus events")
    private String meaningText;

    @JsonProperty("price")
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @Schema(description = "Sale price in VND", example = "150000.00")
    private BigDecimal price;

    @JsonProperty("stockQuantity")
    @Schema(description = "Initial stock quantity; required for ready-stock items", example = "100")
    private Integer stockQuantity;

    @JsonProperty("isPreorder")
    @NotNull(message = "Preorder flag is required")
    @Schema(description = "Whether the item is a preorder item", example = "false")
    private Boolean isPreorder;

    @JsonProperty("imageUrl")
    @Size(max = 2048, message = "Image URL must be at most 2048 characters")
    @Schema(description = "Public image URL from Supabase Storage", example = "https://example.supabase.co/storage/v1/object/public/merch/hoodie.png")
    private String imageUrl;
}