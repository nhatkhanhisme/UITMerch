package com.uitmerch.backend.common.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Pagination metadata for paginated GET responses.
 * 
 * USAGE:
 * Include this in ApiResponse.meta for list endpoints like:
 * - GET /api/v1/public/merch?page=0&pageSize=20
 * - GET /api/v1/customer/collections?page=0&pageSize=10
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Pagination metadata for list responses")
public class PaginationMeta {
    
    @JsonProperty("page")
    @Schema(description = "Current page number (0-indexed)", example = "0")
    private Integer page;
    
    @JsonProperty("pageSize")
    @Schema(description = "Number of items per page", example = "20")
    private Integer pageSize;
    
    @JsonProperty("totalElements")
    @Schema(description = "Total number of items across all pages", example = "150")
    private Long totalElements;
    
    @JsonProperty("totalPages")
    @Schema(description = "Total number of pages", example = "8")
    private Integer totalPages;
    
    @JsonProperty("hasNext")
    @Schema(description = "Whether there are more pages after current", example = "true")
    private Boolean hasNext;
    
    @JsonProperty("hasPrevious")
    @Schema(description = "Whether there are pages before current", example = "false")
    private Boolean hasPrevious;
}
