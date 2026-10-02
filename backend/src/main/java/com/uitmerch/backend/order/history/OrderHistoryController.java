package com.uitmerch.backend.order.history;
import com.uitmerch.backend.common.model.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
@RestController @RequiredArgsConstructor
public class OrderHistoryController {
    private final OrderHistoryService history;
    @GetMapping("/api/v1/customer/orders/{orderId}/history") @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Read your order's transition and pickup history")
    public ApiResponse<Page<OrderHistory>> customer(@RequestAttribute("userId") String user, @PathVariable UUID orderId, Pageable page) {
        return ApiResponse.success("Order history.", history.forCustomer(UUID.fromString(user), orderId, page));
    }
    @GetMapping("/api/v1/organizations/{orgId}/orders/{orderId}/history") @PreAuthorize("hasRole('ORGANIZER')")
    @Operation(summary = "Read the history of an order belonging to your organization")
    public ApiResponse<Page<OrderHistory>> organizer(@RequestAttribute("userId") String user, @PathVariable UUID orgId, @PathVariable UUID orderId, Pageable page) {
        return ApiResponse.success("Order history.", history.forOrganization(UUID.fromString(user), orgId, orderId, page));
    }
}
