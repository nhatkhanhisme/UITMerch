package com.uitmerch.backend.orders.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for checkout.
 * BR04: Idempotency key is required to prevent duplicate orders.
 * BR05: Payment method is always cash on delivery and is not supplied by the client.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Checkout request")
public class CheckoutRequest {

    @JsonProperty("idempotencyKey")
    @NotBlank(message = "Idempotency key is required")
    @Size(min = 1, max = 80, message = "Idempotency key must be between 1 and 80 characters")
    @Schema(description = "Unique idempotency key for checkout", example = "ck_550e8400e29b41d4a716446655440000")
    private String idempotencyKey;
}