package com.uitmerch.backend.orders.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Order line item response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Order item response")
public class OrderItemResponse {

    @JsonProperty("id")
    @Schema(description = "Order item UUID", example = "550e8400-e29b-41d4-a716-446655440001")
    private UUID id;

    @JsonProperty("merchItemId")
    @Schema(description = "Merchandise UUID", example = "550e8400-e29b-41d4-a716-446655440002")
    private UUID merchItemId;

    @JsonProperty("name")
    @Schema(description = "Merchandise name snapshot", example = "UIT Hoodie 2026")
    private String name;

    @JsonProperty("quantity")
    @Schema(description = "Ordered quantity", example = "2")
    private Integer quantity;

    @JsonProperty("unitPrice")
    @Schema(description = "Snapshot unit price", example = "150000.00")
    private BigDecimal unitPrice;

    @JsonProperty("subtotal")
    @Schema(description = "Line total", example = "300000.00")
    private BigDecimal subtotal;
}