package com.uitmerch.backend.orders.controller;

import com.uitmerch.backend.common.model.ApiResponse;
import com.uitmerch.backend.common.util.TraceIdUtil;
import com.uitmerch.backend.orders.dto.CheckoutRequest;
import com.uitmerch.backend.orders.dto.CheckoutResponse;
import com.uitmerch.backend.orders.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Customer checkout endpoint.
 */
@RestController
@RequestMapping("/api/v1/customer/orders")
@PreAuthorize("hasRole('CUSTOMER')")
@Tag(name = "5. Customer Orders", description = "Customer order management endpoints")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @Operation(summary = "Checkout active cart into a COD order", description = "Converts active cart to order with COD payment method. Supports idempotent checkout via idempotencyKey.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Order created successfully", content = @Content(schema = @Schema(implementation = CheckoutResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request or business rule violation (insufficient stock, cart not found)"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - must have CUSTOMER role"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Cart, item, or organization not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflict - cart already checked out or other concurrent modification")
    })
    public ResponseEntity<ApiResponse<CheckoutResponse>> checkout(
        @Valid @RequestBody CheckoutRequest request
    ) {
        CheckoutResponse response = orderService.checkout(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(response, TraceIdUtil.getOrCreateTraceId()));
    }
}