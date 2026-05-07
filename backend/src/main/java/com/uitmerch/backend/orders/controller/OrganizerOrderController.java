package com.uitmerch.backend.orders.controller;

import com.uitmerch.backend.common.model.ApiResponse;
import com.uitmerch.backend.common.util.TraceIdUtil;
import com.uitmerch.backend.orders.dto.UpdateOrderStatusRequest;
import com.uitmerch.backend.orders.dto.UpdateOrderStatusResponse;
import com.uitmerch.backend.orders.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Organizer order management endpoints.
 */
@RestController
@RequestMapping("/api/v1/organizer/orders")
@PreAuthorize("hasRole('ORGANIZER')")
@Tag(name = "6. Organizer Orders", description = "Organizer order management endpoints")
public class OrganizerOrderController {

    private final OrderService orderService;

    public OrganizerOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PatchMapping("/{orderId}/status")
    @Operation(summary = "Update order status", description = "Updates order status with state machine validation (PENDING→CONFIRMED/CANCELLED, CONFIRMED→READY_FOR_PICKUP/CANCELLED, READY_FOR_PICKUP→SUCCESS/CANCELLED). Success status triggers collection creation.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Status updated successfully", content = @Content(schema = @Schema(implementation = UpdateOrderStatusResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid status transition or request validation failed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - must have ORGANIZER role or not order owner"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflict - order already in terminal state")
    })
    public ResponseEntity<ApiResponse<UpdateOrderStatusResponse>> updateOrderStatus(
        @PathVariable UUID orderId,
        @Valid @RequestBody UpdateOrderStatusRequest request
    ) {
        UpdateOrderStatusResponse response = orderService.updateOrderStatus(orderId, request);
        return ResponseEntity.ok(ApiResponse.success(response, TraceIdUtil.getOrCreateTraceId()));
    }
}