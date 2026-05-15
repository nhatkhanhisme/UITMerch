package com.uitmerch.backend.order.controller;

import com.uitmerch.backend.common.model.ApiResponse;
import com.uitmerch.backend.common.model.OrderStatus;
import com.uitmerch.backend.common.model.PaginationMeta;
import com.uitmerch.backend.order.dto.InstantOrderRequest;
import com.uitmerch.backend.order.dto.OrderResponse;
import com.uitmerch.backend.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customer/orders")
@RequiredArgsConstructor
@Tag(name = "Customer", description = "View and place orders for authenticated customers")
@SecurityRequirement(name = "bearerAuth")
public class CustomerOrderController {

    private final OrderService orderService;

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "List customer orders", description = "Returns paginated orders for the authenticated customer. Filter by status using ?status=PENDING.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Orders retrieved"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized — missing or invalid JWT"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — CUSTOMER role required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<ApiResponse<java.util.List<OrderResponse>>> getOrders(
        @RequestParam(required = false) OrderStatus status,
        @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
        @RequestAttribute("userId") String userId
    ) {
        Page<OrderResponse> page = orderService.getCustomerOrders(UUID.fromString(userId), status, pageable);
        return ResponseEntity.ok(ApiResponse.success("Orders retrieved.", page.getContent(), PaginationMeta.from(page)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get order by ID")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order retrieved"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized — missing or invalid JWT"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — CUSTOMER role required or order belongs to another customer"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(
        @PathVariable UUID id,
        @RequestAttribute("userId") String userId
    ) {
        OrderResponse order = orderService.getCustomerOrder(UUID.fromString(userId), id);
        return ResponseEntity.ok(ApiResponse.success("Order retrieved.", order));
    }

    @PostMapping("/instant")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Create instant order", description = "Places an immediate order for a single item without going through the cart.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Order placed successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed — see data for field errors or insufficient stock"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized — missing or invalid JWT"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — CUSTOMER role required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Merch item not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<ApiResponse<OrderResponse>> createInstantOrder(
        @Valid @RequestBody InstantOrderRequest request,
        @RequestAttribute("userId") String userId
    ) {
        OrderResponse order = orderService.createInstantOrder(UUID.fromString(userId), request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Order placed successfully.", order));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Cancel a pending order", description = "Customers may cancel their own PENDING orders. Stock is restored automatically.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order cancelled and stock restored"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Order is not in PENDING status"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Order belongs to another customer"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
        @PathVariable UUID id,
        @RequestAttribute("userId") String userId
    ) {
        OrderResponse order = orderService.cancelCustomerOrder(UUID.fromString(userId), id);
        return ResponseEntity.ok(ApiResponse.success("Order cancelled successfully.", order));
    }
}
