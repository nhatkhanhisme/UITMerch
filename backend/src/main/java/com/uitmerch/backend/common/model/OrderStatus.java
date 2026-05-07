package com.uitmerch.backend.common.model;

/**
 * Order lifecycle states as per SRS BR06.
 * Terminal states: SUCCESS, CANCELLED
 */
public enum OrderStatus {
    PENDING,              // Initial state after COD checkout (FR07)
    CONFIRMED,            // Organizer confirmed order (FR08)
    READY_FOR_PICKUP,     // Order packaged and ready (FR08)
    SUCCESS,              // Pickup completed → added to Collection (BR09, FR09)
    CANCELLED             // Terminal state - order cancelled
}
