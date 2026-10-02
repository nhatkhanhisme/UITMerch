package com.uitmerch.backend.restock;

import com.uitmerch.backend.common.model.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.UUID;

@RestController @RequestMapping("/api/v1/customer/restock-subscriptions")
@PreAuthorize("hasRole('CUSTOMER')") @RequiredArgsConstructor
public class RestockController {
    private final RestockService service;
    @Data public static class SubscribeRequest {
        @NotNull private UUID merchId;
        private boolean emailEnabled = true;
    }
    public record SubscriptionResponse(UUID merchId, boolean emailEnabled, Instant subscribedAt) {
        static SubscriptionResponse from(RestockSubscription s) { return new SubscriptionResponse(s.getMerchId(), s.isEmailEnabled(), s.getSubscribedAt()); }
    }
    @PostMapping @Operation(summary = "Subscribe to future restock alerts; repeated calls update preferences")
    public ApiResponse<SubscriptionResponse> subscribe(@RequestAttribute("userId") String userId, @Valid @RequestBody SubscribeRequest request) {
        return ApiResponse.success("Restock subscription saved.", SubscriptionResponse.from(service.subscribe(UUID.fromString(userId), request.getMerchId(), request.isEmailEnabled())));
    }
    @GetMapping @Operation(summary = "List your enabled restock subscriptions")
    public ApiResponse<Page<SubscriptionResponse>> list(@RequestAttribute("userId") String userId, Pageable page) {
        return ApiResponse.success("Restock subscriptions.", service.list(UUID.fromString(userId), page).map(SubscriptionResponse::from));
    }
    @DeleteMapping("/{merchId}") @Operation(summary = "Unsubscribe from future restock alerts")
    public ApiResponse<Void> unsubscribe(@RequestAttribute("userId") String userId, @PathVariable UUID merchId) {
        service.unsubscribe(UUID.fromString(userId), merchId); return ApiResponse.success("Unsubscribed.", null);
    }
}
