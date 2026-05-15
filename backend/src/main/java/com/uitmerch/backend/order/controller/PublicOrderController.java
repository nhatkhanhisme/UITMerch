package com.uitmerch.backend.order.controller;

import com.uitmerch.backend.common.exception.ValidationException;
import com.uitmerch.backend.common.model.ApiResponse;
import com.uitmerch.backend.common.service.RateLimiterService;
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

@RestController
@RequestMapping("/api/v1/public/orders")
@RequiredArgsConstructor
@Tag(name = "Public", description = "Guest checkout without authentication")
public class PublicOrderController {

    private final OrderService orderService;
    private final RateLimiterService rateLimiterService;

    private static final int GUEST_ORDER_MAX  = 20;
    private static final Duration GUEST_ORDER_WINDOW = Duration.ofHours(1);

    private static String clientIp(HttpServletRequest req) {
        String forwarded = req.getHeader("X-Forwarded-For");
        return (forwarded != null && !forwarded.isBlank())
            ? forwarded.split(",")[0].trim()
            : req.getRemoteAddr();
    }

    @PostMapping
    @Operation(summary = "Guest checkout", description = "Places orders as a guest without requiring an account. Items are grouped by organization.")
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
        if (!rateLimiterService.isAllowed("guest-order:" + clientIp(httpRequest), GUEST_ORDER_MAX, GUEST_ORDER_WINDOW)) {
            throw new ValidationException("Too many orders from this IP. Please try again later.");
        }
        List<OrderResponse> orders = orderService.createGuestOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Guest order placed successfully. " + orders.size() + " order(s) created.", orders));
    }
}
