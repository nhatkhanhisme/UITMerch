package com.uitmerch.backend.orders.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.uitmerch.backend.common.model.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating order status.
 * BR06: Enforces valid status transitions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Update order status request")
public class UpdateOrderStatusRequest {

    @JsonProperty("newStatus")
    @NotNull(message = "New status is required")
    @Schema(description = "Target status for the order", example = "CONFIRMED")
    private OrderStatus newStatus;
}
