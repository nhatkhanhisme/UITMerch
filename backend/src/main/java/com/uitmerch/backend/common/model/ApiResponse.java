package com.uitmerch.backend.common.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Standard API response envelope for all endpoints.
 * 
 * RESPONSE FORMATS:
 * 
 * 1. Successful GET with pagination:
 *    { data: [...], meta: { page, pageSize, totalElements, ... }, traceId: "uuid" }
 * 
 * 2. Successful GET without pagination:
 *    { data: {...}, traceId: "uuid" }
 * 
 * 3. Successful POST/PATCH/DELETE:
 *    { data: {...}, traceId: "uuid" }
 * 
 * 4. Error (all methods):
 *    { data: { errorCode: "SYS_001", message: "...", fieldErrors: [...] }, traceId: "uuid" }
 * 
 * ERROR CODES:
 *   - SYS_001: Request validation failed (MethodArgumentNotValidException)
 *   - SYS_002: Bad request
 *   - SYS_003: Resource not found (404)
 *   - SYS_004: Unauthorized (401)
 *   - SYS_005: Internal server error (500)
 *   - CONFLICT: Resource already exists (409)
 *   - AUTHENTICATION_FAILED: Invalid credentials (401)
 *   - ACCESS_DENIED: Insufficient permissions (403)
 *   - VALIDATION_ERROR: Business logic validation failed (400)
 *   - RESOURCE_NOT_FOUND: Entity not found (404)
 * 
 * @param <T> type of data payload
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard API response envelope for all requests")
public class ApiResponse<T> {
    
    @JsonProperty("data")
    @Schema(description = "Response payload (data or error detail)")
    private T data;
    
    @JsonProperty("meta")
    @Schema(description = "Pagination metadata (for paginated GET responses)")
    private PaginationMeta meta;
    
    @JsonProperty("traceId")
    @Schema(description = "Unique request trace ID for distributed tracing and debugging", example = "550e8400-e29b-41d4-a716-446655440000")
    private String traceId;
    
    /**
     * Helper to create successful response without pagination.
     * Use for: Single object responses, POST/PATCH/DELETE, error responses
     * @param data response payload
     * @param traceId request trace ID
     * @param <T> payload type
     * @return ApiResponse instance
     */
    public static <T> ApiResponse<T> success(T data, String traceId) {
        return ApiResponse.<T>builder()
            .data(data)
            .traceId(traceId)
            .build();
    }
    
    /**
     * Helper to create paginated response with metadata.
     * Use for: List responses (GET /api/v1/public/merch)
     * @param data response payload (typically List<T>)
     * @param meta pagination metadata
     * @param traceId request trace ID
     * @param <T> payload type
     * @return ApiResponse instance
     */
    public static <T> ApiResponse<T> successWithMeta(T data, PaginationMeta meta, String traceId) {
        return ApiResponse.<T>builder()
            .data(data)
            .meta(meta)
            .traceId(traceId)
            .build();
    }
}

