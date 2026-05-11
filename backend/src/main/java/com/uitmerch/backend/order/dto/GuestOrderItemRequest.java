package com.uitmerch.backend.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class GuestOrderItemRequest {

    @NotNull(message = "Merch ID is required")
    private UUID merchId;

    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity;
}
