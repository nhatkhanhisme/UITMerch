package com.uitmerch.backend.order.controller;

import com.uitmerch.backend.common.model.ApiResponse;
import com.uitmerch.backend.common.model.OrderStatus;
import com.uitmerch.backend.common.model.PaginationMeta;
import com.uitmerch.backend.order.dto.OrderResponse;
import com.uitmerch.backend.order.dto.UpdateOrderStatusRequest;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations/{orgId}/orders")
@RequiredArgsConstructor
@Tag(name = "Organizer", description = "Manage incoming orders for the organizer's organization")
@SecurityRequirement(name = "bearerAuth")
public class OrganizerOrderController {

    private final OrderService orderService;

    @GetMapping
    @PreAuthorize("hasRole('ORGANIZER')")
    @Operation(summary = "List organization orders", description = "Returns paginated orders for the organizer's organization. Filter by status using ?status=PENDING.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Orders retrieved"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized — missing or invalid JWT"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — ORGANIZER role required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<ApiResponse<java.util.List<OrderResponse>>> getOrgOrders(
        @PathVariable UUID orgId,
        @RequestParam(required = false) OrderStatus status,
        @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
        @RequestAttribute("userId") String userId
    ) {
        Page<OrderResponse> page = orderService.getOrgOrders(UUID.fromString(userId), orgId, status, pageable);
        return ResponseEntity.ok(ApiResponse.success("Orders retrieved.", page.getContent(), PaginationMeta.from(page)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZER')")
    @Operation(summary = "Get organization order by ID")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order retrieved"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized — missing or invalid JWT"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — ORGANIZER role required or order belongs to another organization"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<ApiResponse<OrderResponse>> getOrgOrder(
        @PathVariable UUID orgId,
        @PathVariable UUID id,
        @RequestAttribute("userId") String userId
    ) {
        OrderResponse order = orderService.getOrgOrder(UUID.fromString(userId), orgId, id);
        return ResponseEntity.ok(ApiResponse.success("Order retrieved.", order));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ORGANIZER')")
    @Operation(
        summary = "Update order status",
        description = "Valid transitions (BR05): PENDING→CONFIRMED, PENDING→CANCELLED, CONFIRMED→READY_FOR_PICKUP, CONFIRMED→CANCELLED, READY_FOR_PICKUP→SUCCESS"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order status updated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed or invalid status transition"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized — missing or invalid JWT"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — ORGANIZER role required or order belongs to another organization"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
        @PathVariable UUID orgId,
        @PathVariable UUID id,
        @Valid @RequestBody UpdateOrderStatusRequest request,
        @RequestAttribute("userId") String userId
    ) {
        OrderResponse order = orderService.updateOrderStatus(UUID.fromString(userId), orgId, id, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success("Order status updated.", order));
    }
}
