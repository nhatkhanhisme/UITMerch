package com.uitmerch.backend.order.dto;

import com.uitmerch.backend.common.model.OrderStatus;
import com.uitmerch.backend.common.model.PaymentMethod;
import com.uitmerch.backend.common.model.PaymentStatus;
import com.uitmerch.backend.order.entity.Order;
import com.uitmerch.backend.order.entity.OrderItem;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class OrderResponse {

    private UUID id;
    private UUID userId;
    private UUID orgId;
    private String guestName;
    private String guestEmail;
    private String guestPhone;
    private String guestAddress;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private String note;
    private List<OrderItemResponse> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static OrderResponse from(Order order, List<OrderItem> orderItems) {
        List<OrderItemResponse> items = orderItems.stream()
            .map(item -> OrderItemResponse.builder()
                .id(item.getId())
                .orderId(item.getOrderId())
                .merchId(item.getMerchId())
                .merchName(item.getMerchName())
                .unitPrice(item.getUnitPrice())
                .quantity(item.getQuantity())
                .subtotal(item.getSubtotal())
                .createdAt(item.getCreatedAt())
                .build())
            .toList();

        return OrderResponse.builder()
            .id(order.getId())
            .userId(order.getUserId())
            .orgId(order.getOrgId())
            .guestName(order.getGuestName())
            .guestEmail(order.getGuestEmail())
            .guestPhone(order.getGuestPhone())
            .guestAddress(order.getGuestAddress())
            .totalAmount(order.getTotalAmount())
            .status(order.getStatus())
            .paymentMethod(order.getPaymentMethod())
            .paymentStatus(order.getPaymentStatus())
            .note(order.getNote())
            .items(items)
            .createdAt(order.getCreatedAt())
            .updatedAt(order.getUpdatedAt())
            .build();
    }
}
