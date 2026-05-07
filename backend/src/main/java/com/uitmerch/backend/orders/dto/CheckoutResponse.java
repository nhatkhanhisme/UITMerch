package com.uitmerch.backend.orders.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.uitmerch.backend.common.model.OrderStatus;
import com.uitmerch.backend.common.model.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Checkout response returned after order creation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Checkout response")
public class CheckoutResponse {

    @JsonProperty("id")
    @Schema(description = "Order UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    @JsonProperty("orderCode")
    @Schema(description = "Human-friendly order code", example = "ORD-20260506-ABC123")
    private String orderCode;

    @JsonProperty("organizationId")
    @Schema(description = "Organization UUID", example = "550e8400-e29b-41d4-a716-446655440003")
    private UUID organizationId;

    @JsonProperty("totalAmount")
    @Schema(description = "Total order amount", example = "300000.00")
    private BigDecimal totalAmount;

    @JsonProperty("paymentMethod")
    @Schema(description = "Always CASH_ON_DELIVERY", example = "CASH_ON_DELIVERY")
    private PaymentMethod paymentMethod;

    @JsonProperty("status")
    @Schema(description = "Order status", example = "PENDING")
    private OrderStatus status;

    @JsonProperty("items")
    @Schema(description = "Order items")
    private List<OrderItemResponse> items;

    @JsonProperty("placedAt")
    @Schema(description = "Placement timestamp", example = "2026-05-06T10:30:00Z")
    private Instant placedAt;

    @JsonProperty("createdAt")
    @Schema(description = "Creation timestamp", example = "2026-05-06T10:30:00Z")
    private Instant createdAt;

    @JsonProperty("updatedAt")
    @Schema(description = "Last update timestamp", example = "2026-05-06T10:30:00Z")
    private Instant updatedAt;
}