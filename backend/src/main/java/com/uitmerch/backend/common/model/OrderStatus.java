package com.uitmerch.backend.common.model;

/**
 * Campus-pickup order lifecycle. Terminal states: COMPLETED, CANCELLED.
 * PENDING → CONFIRMED → READY → COMPLETED
 *                              ↘ CANCELLED (customer: PENDING only; organizer: PENDING or CONFIRMED)
 */
public enum OrderStatus {
    PENDING,    // placed, awaiting organizer confirmation
    CONFIRMED,  // organizer confirmed
    READY,      // pickup schedule created — customer can come collect
    COMPLETED,  // organizer checked in the order code
    CANCELLED   // terminal — stock restored
}
