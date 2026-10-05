package com.uitmerch.backend.order.controller;

import com.uitmerch.backend.common.exception.ValidationException;
import com.uitmerch.backend.common.model.ApiResponse;
import com.uitmerch.backend.common.service.RateLimiterService;
import com.uitmerch.backend.common.util.IpUtil;
import com.uitmerch.backend.order.dto.GuestOrderRequest;
import com.uitmerch.backend.order.dto.OrderResponse;
import com.uitmerch.backend.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/public/orders")
@RequiredArgsConstructor
@Tag(name = "Public", description = "Guest and customer checkout")
public class PublicOrderController {

    private final OrderService orderService;
    private final RateLimiterService rateLimiterService;
    private final IpUtil ipUtil;

    private static final int GUEST_ORDER_MAX  = 20;
    private static final Duration GUEST_ORDER_WINDOW = Duration.ofHours(1);

    @PostMapping
    @Operation(summary = "Guest checkout", description = "Places guest orders anonymously or account-linked orders for an authenticated CUSTOMER. Items are grouped by organization.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Guest order placed successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed — see data for field errors or insufficient stock"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Merch item not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<ApiResponse<List<OrderResponse>>> guestCheckout(
        @Valid @RequestBody GuestOrderRequest request,
        HttpServletRequest httpRequest
    ) {
        if (!rateLimiterService.isAllowed("guest-order:" + ipUtil.extractClientIp(httpRequest), GUEST_ORDER_MAX, GUEST_ORDER_WINDOW)) {
            throw new com.uitmerch.backend.common.exception.RateLimitException("Too many orders from this IP. Please try again later.",3600);
        }
        Object authenticatedId = httpRequest.getAttribute("userId");
        List<OrderResponse> orders;
        if (authenticatedId != null) {
            if (!"CUSTOMER".equals(httpRequest.getAttribute("role"))) {
                throw new com.uitmerch.backend.common.exception.ForbiddenException("Only customers can place account-linked orders.");
            }
            orders = orderService.createPublicOrder(UUID.fromString(authenticatedId.toString()), request);
        } else {
            orders = orderService.createGuestOrder(request);
        }
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Guest order placed successfully. " + orders.size() + " order(s) created.", orders));
    }

    @GetMapping("/{orderId}")
    @Operation(
        summary = "Track a guest order",
        description = "Returns order details for a guest order. Requires the guest email used at checkout for verification."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found or email does not match")
    })
    public ResponseEntity<ApiResponse<OrderResponse>> trackGuestOrder(
        @PathVariable UUID orderId,
        @RequestParam String email
    ) {
        throw new com.uitmerch.backend.common.exception.ResourceNotFoundException("Order",orderId.toString());
    }
}
