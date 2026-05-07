package com.uitmerch.backend.orders.service;

import com.uitmerch.backend.orders.dto.CheckoutRequest;
import com.uitmerch.backend.orders.dto.CheckoutResponse;
import com.uitmerch.backend.orders.dto.UpdateOrderStatusRequest;
import com.uitmerch.backend.orders.dto.UpdateOrderStatusResponse;

import java.util.UUID;

/**
 * Order service contract for checkout and status updates.
 */
public interface OrderService {

    CheckoutResponse checkout(CheckoutRequest request);

    UpdateOrderStatusResponse updateOrderStatus(UUID orderId, UpdateOrderStatusRequest request);
}