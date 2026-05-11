package com.uitmerch.backend.cart.dto;

import com.uitmerch.backend.merch.dto.MerchResponse;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class CartItemResponse {

    private UUID id;
    private UUID cartId;
    private MerchResponse merch;
    private int quantity;
    private BigDecimal subtotal;
    private LocalDateTime createdAt;
}
