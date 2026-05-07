package com.uitmerch.backend.orders.util;

import com.uitmerch.backend.common.model.OrderStatus;
import com.uitmerch.backend.common.exception.ValidationException;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Validates order status transitions as per BR06.
 * State machine:
 * PENDING -> {CONFIRMED, CANCELLED}
 * CONFIRMED -> {READY_FOR_PICKUP, CANCELLED}
 * READY_FOR_PICKUP -> {SUCCESS, CANCELLED}
 * SUCCESS (terminal)
 * CANCELLED (terminal)
 */
public class OrderStatusTransitionValidator {

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(OrderStatus.class);

    static {
        // PENDING can transition to CONFIRMED or CANCELLED
        ALLOWED_TRANSITIONS.put(OrderStatus.PENDING, Set.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED));

        // CONFIRMED can transition to READY_FOR_PICKUP or CANCELLED
        ALLOWED_TRANSITIONS.put(OrderStatus.CONFIRMED, Set.of(OrderStatus.READY_FOR_PICKUP, OrderStatus.CANCELLED));

        // READY_FOR_PICKUP can transition to SUCCESS or CANCELLED
        ALLOWED_TRANSITIONS.put(OrderStatus.READY_FOR_PICKUP, Set.of(OrderStatus.SUCCESS, OrderStatus.CANCELLED));

        // Terminal states: no transitions allowed
        ALLOWED_TRANSITIONS.put(OrderStatus.SUCCESS, new HashSet<>());
        ALLOWED_TRANSITIONS.put(OrderStatus.CANCELLED, new HashSet<>());
    }

    /**
     * Validates if the transition from currentStatus to newStatus is allowed.
     * @throws ValidationException if the transition is not allowed
     */
    public static void validateTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        Set<OrderStatus> allowedTargets = ALLOWED_TRANSITIONS.get(currentStatus);

        if (!allowedTargets.contains(newStatus)) {
            throw new ValidationException(
                String.format("Invalid order status transition: %s -> %s", currentStatus, newStatus)
            );
        }
    }
}
