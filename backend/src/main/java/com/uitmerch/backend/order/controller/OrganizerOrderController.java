package com.uitmerch.backend.order.controller;

import com.uitmerch.backend.common.model.ApiResponse;
import com.uitmerch.backend.common.model.OrderStatus;
import com.uitmerch.backend.common.model.PaginationMeta;
import com.uitmerch.backend.order.dto.*;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations/{orgId}")
@RequiredArgsConstructor
@Tag(name = "Organizer", description = "Manage incoming orders and pickup schedules for the organizer's organization")
@SecurityRequirement(name = "bearerAuth")
public class OrganizerOrderController {

    private final OrderService orderService;

    // ── Order list & detail ─────────────────────────────────────────────── //

    @GetMapping("/orders")
    @PreAuthorize("hasRole('ORGANIZER')")
    @Operation(summary = "List organization orders", description = "Filter by ?status=PENDING.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Orders retrieved"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ORGANIZER role required")
    })
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrgOrders(
        @PathVariable UUID orgId,
        @RequestParam(required = false) OrderStatus status,
        @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
        @RequestAttribute("userId") String userId
    ) {
        Page<OrderResponse> page = orderService.getOrgOrders(UUID.fromString(userId), orgId, status, pageable);
        return ResponseEntity.ok(ApiResponse.success("Orders retrieved.", page.getContent(), PaginationMeta.from(page)));
    }

    @GetMapping("/orders/{id}")
    @PreAuthorize("hasRole('ORGANIZER')")
    @Operation(summary = "Get order by ID")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrgOrder(
        @PathVariable UUID orgId,
        @PathVariable UUID id,
        @RequestAttribute("userId") String userId
    ) {
        OrderResponse order = orderService.getOrgOrder(UUID.fromString(userId), orgId, id);
        return ResponseEntity.ok(ApiResponse.success("Order retrieved.", order));
    }

    // ── Status update (non-cancel transitions) ──────────────────────────── //

    @PatchMapping("/orders/{id}/status")
    @PreAuthorize("hasRole('ORGANIZER')")
    @Operation(
        summary = "Update order status",
        description = "Non-cancel transitions: PENDING→CONFIRMED, CONFIRMED→READY, READY→COMPLETED. Use /cancel for cancellations."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Status updated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid transition"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found")
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

    // ── Check-in (READY → COMPLETED) ────────────────────────────────────── //

    @PatchMapping("/orders/{id}/checkin")
    @PreAuthorize("hasRole('ORGANIZER')")
    @Operation(summary = "Check-in order", description = "Marks a READY order as COMPLETED when the customer shows up and presents their order code.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order completed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Order not READY"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<ApiResponse<OrderResponse>> checkIn(
        @PathVariable UUID orgId,
        @PathVariable UUID id,
        @RequestAttribute("userId") String userId
    ) {
        OrderResponse order = orderService.checkInOrder(UUID.fromString(userId), orgId, id);
        return ResponseEntity.ok(ApiResponse.success("Order checked in.", order));
    }

    // ── Organizer cancel (PENDING or CONFIRMED) ─────────────────────────── //

    @PatchMapping("/orders/{id}/cancel")
    @PreAuthorize("hasRole('ORGANIZER')")
    @Operation(
        summary = "Cancel order",
        description = "Organizers may cancel PENDING or CONFIRMED orders. A cancellation reason is mandatory."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order cancelled"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Order not cancellable or missing reason"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
        @PathVariable UUID orgId,
        @PathVariable UUID id,
        @Valid @RequestBody CancelOrderRequest request,
        @RequestAttribute("userId") String userId
    ) {
        OrderResponse order = orderService.cancelOrgOrder(UUID.fromString(userId), orgId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Order cancelled.", order));
    }

    // ── Pickup schedules ────────────────────────────────────────────────── //

    @PostMapping("/pickup-schedules")
    @PreAuthorize("hasRole('ORGANIZER')")
    @Operation(
        summary = "Create pickup schedule",
        description = "Creates a campus pickup schedule and moves all specified CONFIRMED orders to READY status. Customers are notified automatically."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Pickup schedule created"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed or orders not CONFIRMED"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<ApiResponse<PickupScheduleResponse>> createPickupSchedule(
        @PathVariable UUID orgId,
        @Valid @RequestBody PickupScheduleRequest request,
        @RequestAttribute("userId") String userId
    ) {
        PickupScheduleResponse schedule = orderService.createPickupSchedule(UUID.fromString(userId), orgId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Pickup schedule created.", schedule));
    }

    @GetMapping("/pickup-schedules")
    @PreAuthorize("hasRole('ORGANIZER')")
    @Operation(summary = "List pickup schedules for this organization")
    public ResponseEntity<ApiResponse<List<PickupScheduleResponse>>> getPickupSchedules(
        @PathVariable UUID orgId,
        @ParameterObject @PageableDefault(size = 20, sort = "pickupDate", direction = Sort.Direction.DESC) Pageable pageable,
        @RequestAttribute("userId") String userId
    ) {
        Page<PickupScheduleResponse> page = orderService.getPickupSchedules(UUID.fromString(userId), orgId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Pickup schedules retrieved.", page.getContent(), PaginationMeta.from(page)));
    }

    @GetMapping("/pickup-schedules/{scheduleId}/orders")
    @PreAuthorize("hasRole('ORGANIZER')")
    @Operation(summary = "Get check-in list for a pickup schedule")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getPickupScheduleOrders(
        @PathVariable UUID orgId,
        @PathVariable UUID scheduleId,
        @RequestAttribute("userId") String userId
    ) {
        List<OrderResponse> orders = orderService.getPickupScheduleOrders(UUID.fromString(userId), orgId, scheduleId);
        return ResponseEntity.ok(ApiResponse.success("Check-in list retrieved.", orders));
    }
}
