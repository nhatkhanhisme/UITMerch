package com.uitmerch.backend.cart.dto;

import com.uitmerch.backend.common.model.CartStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class CartResponse {

    private UUID id;
    private UUID userId;
    private CartStatus status;
    private List<CartItemResponse> items;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
