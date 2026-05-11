package com.uitmerch.backend.order.controller;

import com.uitmerch.backend.common.model.ApiResponse;
import com.uitmerch.backend.common.model.OrderStatus;
import com.uitmerch.backend.common.model.PaginationMeta;
import com.uitmerch.backend.order.dto.OrderResponse;
import com.uitmerch.backend.order.dto.UpdateOrderStatusRequest;
import com.uitmerch.backend.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations/orders")
@RequiredArgsConstructor
@Tag(name = "Organizer — Orders", description = "Manage incoming orders for the organizer's organization")
@SecurityRequirement(name = "bearerAuth")
public class OrganizerOrderController {

    private final OrderService orderService;

    @GetMapping
    @PreAuthorize("hasRole('ORGANIZER')")
    @Operation(summary = "List organization orders", description = "Returns paginated orders for the organizer's organization. Filter by status using ?status=PENDING.")
    public ResponseEntity<ApiResponse<java.util.List<OrderResponse>>> getOrgOrders(
        @RequestParam(required = false) OrderStatus status,
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
        @RequestAttribute("userId") String userId
    ) {
        Page<OrderResponse> page = orderService.getOrgOrders(UUID.fromString(userId), status, pageable);
        return ResponseEntity.ok(ApiResponse.success("Orders retrieved.", page.getContent(), PaginationMeta.from(page)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZER')")
    @Operation(summary = "Get organization order by ID")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrgOrder(
        @PathVariable UUID id,
        @RequestAttribute("userId") String userId
    ) {
        OrderResponse order = orderService.getOrgOrder(UUID.fromString(userId), id);
        return ResponseEntity.ok(ApiResponse.success("Order retrieved.", order));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ORGANIZER')")
    @Operation(
        summary = "Update order status",
        description = "Valid transitions (BR05): PENDING→CONFIRMED, PENDING→CANCELLED, CONFIRMED→READY_FOR_PICKUP, CONFIRMED→CANCELLED, READY_FOR_PICKUP→SUCCESS"
    )
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateOrderStatusRequest request,
        @RequestAttribute("userId") String userId
    ) {
        OrderResponse order = orderService.updateOrderStatus(UUID.fromString(userId), id, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success("Order status updated.", order));
    }
}
