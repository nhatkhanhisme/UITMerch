package com.uitmerch.backend.orders.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.uitmerch.backend.common.model.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for order status update.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Update order status response")
public class UpdateOrderStatusResponse {

    @JsonProperty("id")
    @Schema(description = "Order UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    @JsonProperty("orderCode")
    @Schema(description = "Human-friendly order code", example = "ORD-ABC123")
    private String orderCode;

    @JsonProperty("organizationId")
    @Schema(description = "Organization UUID", example = "550e8400-e29b-41d4-a716-446655440003")
    private UUID organizationId;

    @JsonProperty("status")
    @Schema(description = "Updated order status", example = "CONFIRMED")
    private OrderStatus status;

    @JsonProperty("updatedAt")
    @Schema(description = "Last update timestamp", example = "2026-05-06T10:35:00Z")
    private Instant updatedAt;
}
