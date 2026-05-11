package com.uitmerch.backend.order.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class OrderItemResponse {

    private UUID id;
    private UUID orderId;
    private UUID merchId;
    private String merchName;
    private BigDecimal unitPrice;
    private int quantity;
    private BigDecimal subtotal;
    private LocalDateTime createdAt;
}
