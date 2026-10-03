package com.uitmerch.backend.following;
import com.uitmerch.backend.common.model.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.UUID;
@RestController @RequestMapping("/api/v1/customer/following")
@PreAuthorize("hasRole('CUSTOMER')") @RequiredArgsConstructor
public class FollowController {
    private final FollowService service;
    public record FollowResponse(UUID orgId, boolean notifyMerch, boolean notifyEvents, boolean emailEnabled, Instant followedAt) {
        static FollowResponse from(OrganizationFollow f) { return new FollowResponse(f.getOrgId(), f.isNotifyMerch(), f.isNotifyEvents(), f.isEmailEnabled(), f.getFollowedAt()); }
    }
    @PostMapping("/{orgId}") @Operation(summary = "Follow an active organization; email is opt-in")
    public ApiResponse<FollowResponse> follow(@RequestAttribute("userId") String user, @PathVariable UUID orgId, @RequestBody(required = false) FollowPreferences preferences) {
        return ApiResponse.success("Following.", FollowResponse.from(service.follow(UUID.fromString(user), orgId, preferences)));
    }
    @PatchMapping("/{orgId}") @Operation(summary = "Update selected notification preferences for an existing follow")
    public ApiResponse<FollowResponse> preferences(@RequestAttribute("userId") String user, @PathVariable UUID orgId, @RequestBody FollowPreferences preferences) {
        return ApiResponse.success("Preferences saved.", FollowResponse.from(service.preferences(UUID.fromString(user), orgId, preferences)));
    }
    @DeleteMapping("/{orgId}") @Operation(summary = "Unfollow and suppress future publication alerts")
    public ApiResponse<Void> unfollow(@RequestAttribute("userId") String user, @PathVariable UUID orgId) {
        service.unfollow(UUID.fromString(user), orgId); return ApiResponse.success("Unfollowed.", null);
    }
    @GetMapping @Operation(summary = "List your followed organizations and preferences")
    public ApiResponse<Page<FollowDetails>> list(@RequestAttribute("userId") String user, Pageable page) {
        return ApiResponse.success("Following.", service.listDetails(UUID.fromString(user), page));
    }
}
