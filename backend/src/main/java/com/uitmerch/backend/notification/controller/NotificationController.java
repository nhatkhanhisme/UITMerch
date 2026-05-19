package com.uitmerch.backend.notification.controller;

import com.uitmerch.backend.common.model.ApiResponse;
import com.uitmerch.backend.common.model.PaginationMeta;
import com.uitmerch.backend.notification.dto.NotificationResponse;
import com.uitmerch.backend.notification.service.NotificationService;
import com.uitmerch.backend.notification.service.SseEmitterManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customer/notifications")
@RequiredArgsConstructor
@Tag(name = "Customer")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;
    private final SseEmitterManager sseEmitterManager;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "SSE stream — real-time notification push for the authenticated customer")
    public SseEmitter stream(@RequestAttribute("userId") String userId) {
        return sseEmitterManager.add(UUID.fromString(userId));
    }

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "List in-app notifications for the authenticated customer")
    public ResponseEntity<ApiResponse<java.util.List<NotificationResponse>>> getNotifications(
        @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
        @RequestAttribute("userId") String userId
    ) {
        Page<NotificationResponse> page = notificationService.getForUser(UUID.fromString(userId), pageable);
        return ResponseEntity.ok(ApiResponse.success("Notifications retrieved.", page.getContent(), PaginationMeta.from(page)));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get unread notification count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(
        @RequestAttribute("userId") String userId
    ) {
        long count = notificationService.countUnread(UUID.fromString(userId));
        return ResponseEntity.ok(ApiResponse.success("Unread count retrieved.", Map.of("unreadCount", count)));
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Mark a notification as read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markRead(
        @PathVariable UUID id,
        @RequestAttribute("userId") String userId
    ) {
        NotificationResponse n = notificationService.markRead(UUID.fromString(userId), id);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read.", n));
    }

    @PatchMapping("/read-all")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Mark all notifications as read")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> markAllRead(
        @RequestAttribute("userId") String userId
    ) {
        int updated = notificationService.markAllRead(UUID.fromString(userId));
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read.", Map.of("updated", updated)));
    }
}
