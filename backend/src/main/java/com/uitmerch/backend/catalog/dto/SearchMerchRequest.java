package com.uitmerch.backend.catalog.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Query DTO for public merch search with pagination.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Search merchandise request")
public class SearchMerchRequest {

    @JsonProperty("keyword")
    @Size(max = 255, message = "Keyword must be at most 255 characters")
    @Schema(description = "Search term matched against name, meaning, and organization", example = "hoodie")
    private String keyword;

    @JsonProperty("organizationId")
    @Schema(description = "Filter by organization ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID organizationId;

    @JsonProperty("isPreorder")
    @Schema(description = "Filter by preorder state", example = "false")
    private Boolean isPreorder;

    @JsonProperty("page")
    @Min(value = 0, message = "Page must be greater than or equal to 0")
    @Schema(description = "Zero-based page index", example = "0")
    private Integer page;

    @JsonProperty("pageSize")
    @Min(value = 1, message = "Page size must be at least 1")
    @Max(value = 100, message = "Page size must be at most 100")
    @Schema(description = "Number of items per page", example = "20")
    private Integer pageSize;

    @JsonProperty("sortBy")
    @Pattern(regexp = "^(name|price|createdAt)$", message = "Sort by must be name, price, or createdAt")
    @Schema(description = "Sort field", example = "createdAt")
    private String sortBy;

    @JsonProperty("sortOrder")
    @Pattern(regexp = "^(asc|desc)$", message = "Sort order must be asc or desc")
    @Schema(description = "Sort direction", example = "desc")
    private String sortOrder;
}