package com.uitmerch.backend.orders.event;

import java.util.UUID;

/**
 * Application event published when an order reaches SUCCESS.
 */
public record OrderSucceededEvent(UUID orderId) {
}