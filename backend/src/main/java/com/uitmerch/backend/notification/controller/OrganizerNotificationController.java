package com.uitmerch.backend.notification.controller;

import com.uitmerch.backend.notification.service.SseEmitterManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizer/notifications")
@RequiredArgsConstructor
@Tag(name = "Organizer")
@SecurityRequirement(name = "bearerAuth")
public class OrganizerNotificationController {

    private final SseEmitterManager sseEmitterManager;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasRole('ORGANIZER')")
    @Operation(summary = "SSE stream — real-time new-order push for the authenticated organizer")
    public SseEmitter stream(@RequestAttribute("userId") String userId) {
        return sseEmitterManager.add(UUID.fromString(userId));
    }
}
