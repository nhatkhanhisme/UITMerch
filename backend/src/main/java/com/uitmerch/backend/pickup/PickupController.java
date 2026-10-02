package com.uitmerch.backend.pickup;

import com.uitmerch.backend.common.exception.RateLimitException;
import com.uitmerch.backend.common.model.ApiResponse;
import com.uitmerch.backend.common.service.RateLimiterService;
import com.uitmerch.backend.common.util.IpUtil;
import com.uitmerch.backend.order.dto.OrderResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.Duration;
import java.util.UUID;

@RestController @RequiredArgsConstructor
public class PickupController {
    private final PickupService pickup;
    private final RateLimiterService rates;
    private final IpUtil ips;
    public record ScanRequest(@NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{43}") String token, UUID pickupScheduleId) {}
    public record ReceiptRequest(@NotBlank @Email @Size(max = 255) String email) {}
    public record GuestTokenRequest(@NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{43}") String receiptToken) {}

    @PostMapping("/api/v1/customer/orders/{orderId}/pickup-token") @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Issue a 30-minute pickup credential; reissue invalidates the previous QR")
    public ApiResponse<PickupService.IssuedToken> issue(@RequestAttribute("userId") String user, @PathVariable UUID orderId) {
        limit("pickup-issue:" + user, 20, 60);
        return ApiResponse.success("Pickup token issued.", pickup.issue(UUID.fromString(user), orderId));
    }
    @PostMapping("/api/v1/organizations/{orgId}/orders/pickup/verify") @PreAuthorize("hasRole('ORGANIZER')")
    @Operation(summary = "Verify a QR for your organization and matching pickup schedule without consuming it")
    public ApiResponse<OrderResponse> verify(@RequestAttribute("userId") String user, @PathVariable UUID orgId, @Valid @RequestBody ScanRequest request) {
        limit("pickup-scan:" + user, 120, 60);
        return ApiResponse.success("Pickup token valid.", pickup.verify(UUID.fromString(user), orgId, request.token(), request.pickupScheduleId()));
    }
    @PostMapping("/api/v1/organizations/{orgId}/orders/pickup/checkin") @PreAuthorize("hasRole('ORGANIZER')")
    @Operation(summary = "Consume a QR and complete its order atomically")
    public ApiResponse<OrderResponse> checkIn(@RequestAttribute("userId") String user, @PathVariable UUID orgId, @Valid @RequestBody ScanRequest request) {
        limit("pickup-scan:" + user, 120, 60);
        return ApiResponse.success("Pickup completed.", pickup.checkIn(UUID.fromString(user), orgId, request.token(), request.pickupScheduleId()));
    }
    @PostMapping("/api/v1/public/orders/{orderId}/pickup-receipt")
    @Operation(summary = "Email a guest-only, 15-minute pickup receipt credential; response never reveals whether an order matches")
    public ResponseEntity<ApiResponse<Void>> receipt(@PathVariable UUID orderId, @Valid @RequestBody ReceiptRequest request, HttpServletRequest http) {
        limit("guest-pickup-ip:" + ips.extractClientIp(http), 5, 60);
        limit("guest-pickup-order:" + orderId, 3, 900);
        pickup.requestGuestReceipt(orderId, request.email());
        return ResponseEntity.accepted().body(ApiResponse.success("If the order is eligible, a receipt was emailed.", null));
    }
    @PostMapping("/api/v1/public/orders/{orderId}/pickup-token")
    @Operation(summary = "Exchange an emailed guest receipt once for a pickup QR credential")
    public ApiResponse<PickupService.IssuedToken> guestToken(@PathVariable UUID orderId, @Valid @RequestBody GuestTokenRequest request, HttpServletRequest http) {
        limit("guest-pickup-token:" + ips.extractClientIp(http), 20, 60);
        return ApiResponse.success("Pickup token issued.", pickup.issueForGuest(orderId, request.receiptToken()));
    }
    private void limit(String key, int count, int seconds) {
        if (!rates.isAllowed(key, count, Duration.ofSeconds(seconds))) throw new RateLimitException("Too many pickup requests.", seconds);
    }
}
