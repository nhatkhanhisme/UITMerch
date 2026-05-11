package com.uitmerch.backend.common.model;

/**
 * Payment lifecycle states for orders.
 */
public enum PaymentStatus {
    PENDING,    // Payment not yet received
    PAID,       // Payment confirmed
    FAILED,     // Payment attempt failed
    REFUNDED    // Payment was refunded
}
