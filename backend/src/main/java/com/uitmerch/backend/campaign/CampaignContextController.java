package com.uitmerch.backend.campaign;

import com.uitmerch.backend.common.model.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CampaignContextController {
    private final CampaignReadService reads;

    @GetMapping("/public/merch/{merchId}/purchase-context")
    public ApiResponse<CampaignReadService.PurchaseContext> purchase(@PathVariable UUID merchId) {
        return ApiResponse.success("Purchase context.", reads.purchaseContext(merchId));
    }

    @GetMapping("/customer/orders/{orderId}/campaign-context")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResponse<CampaignReadService.OrderContext> customer(@RequestAttribute("userId") String user, @PathVariable UUID orderId) {
        return ApiResponse.success("Campaign context.", reads.customerOrder(UUID.fromString(user), orderId));
    }

    @GetMapping("/organizations/{orgId}/orders/{orderId}/campaign-context")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ApiResponse<CampaignReadService.OrderContext> organizer(@RequestAttribute("userId") String user, @PathVariable UUID orgId, @PathVariable UUID orderId) {
        return ApiResponse.success("Campaign context.", reads.organizationOrder(UUID.fromString(user), orgId, orderId));
    }

    @GetMapping("/organizations/{orgId}/campaigns/{campaignId}")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ApiResponse<CampaignResponse> detail(@RequestAttribute("userId") String user, @PathVariable UUID orgId, @PathVariable UUID campaignId) {
        return ApiResponse.success("Campaign.", reads.organizationDetail(UUID.fromString(user), orgId, campaignId));
    }
}
