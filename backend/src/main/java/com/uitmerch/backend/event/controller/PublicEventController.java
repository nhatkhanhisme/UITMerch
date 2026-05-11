package com.uitmerch.backend.event.controller;

import com.uitmerch.backend.common.model.ApiResponse;
import com.uitmerch.backend.common.model.PaginationMeta;
import com.uitmerch.backend.event.dto.EventResponse;
import com.uitmerch.backend.event.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/public/events")
@RequiredArgsConstructor
@Tag(name = "Public — Events", description = "Browse published events")
public class PublicEventController {

    private final EventService eventService;

    @GetMapping
    @Operation(summary = "List published events")
    public ResponseEntity<ApiResponse<List<EventResponse>>> getPublicEvents(
        @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<EventResponse> page = eventService.getPublicEvents(pageable);
        return ResponseEntity.ok(
            ApiResponse.success("Events retrieved.", page.getContent(), PaginationMeta.from(page))
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a published event by ID")
    public ResponseEntity<ApiResponse<EventResponse>> getPublicEvent(@PathVariable UUID id) {
        EventResponse response = eventService.getPublicEvent(id);
        return ResponseEntity.ok(ApiResponse.success("Event retrieved.", response));
    }
}
