package com.uitmerch.backend.event.controller;

import com.uitmerch.backend.common.model.ApiResponse;
import com.uitmerch.backend.common.model.PaginationMeta;
import com.uitmerch.backend.event.dto.AttachMerchRequest;
import com.uitmerch.backend.event.dto.CreateEventRequest;
import com.uitmerch.backend.event.dto.EventResponse;
import com.uitmerch.backend.event.dto.UpdateEventRequest;
import com.uitmerch.backend.event.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations/events")
@RequiredArgsConstructor
@Tag(name = "Organizer — Events", description = "Manage own organization events")
@SecurityRequirement(name = "bearerAuth")
public class EventController {

    private final EventService eventService;

    @PostMapping
    @PreAuthorize("hasRole('ORGANIZER')")
    @Operation(summary = "Create an event")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Event created"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed — see data for field errors"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized — missing or invalid JWT"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — ORGANIZER role required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Organization not found for this user"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<ApiResponse<EventResponse>> createEvent(
        @Valid @RequestBody CreateEventRequest request,
        @RequestAttribute("userId") String userId
    ) {
        EventResponse response = eventService.createEvent(UUID.fromString(userId), request);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success("Event created.", response));
    }

    @GetMapping
    @PreAuthorize("hasRole('ORGANIZER')")
    @Operation(summary = "List own events")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Events retrieved"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized — missing or invalid JWT"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — ORGANIZER role required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<ApiResponse<List<EventResponse>>> getOwnEvents(
        @ParameterObject @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
        @RequestAttribute("userId") String userId
    ) {
        Page<EventResponse> page = eventService.getOwnEvents(UUID.fromString(userId), pageable);
        return ResponseEntity.ok(
            ApiResponse.success("Events retrieved.", page.getContent(), PaginationMeta.from(page))
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZER')")
    @Operation(summary = "Get own event by ID")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Event retrieved"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized — missing or invalid JWT"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — ORGANIZER role required or event belongs to another organization"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Event not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<ApiResponse<EventResponse>> getOwnEvent(
        @PathVariable UUID id,
        @RequestAttribute("userId") String userId
    ) {
        EventResponse response = eventService.getOwnEvent(UUID.fromString(userId), id);
        return ResponseEntity.ok(ApiResponse.success("Event retrieved.", response));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZER')")
    @Operation(summary = "Update an event")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Event updated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed — see data for field errors"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized — missing or invalid JWT"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — ORGANIZER role required or event belongs to another organization"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Event not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<ApiResponse<EventResponse>> updateEvent(
        @PathVariable UUID id,
        @RequestBody UpdateEventRequest request,
        @RequestAttribute("userId") String userId
    ) {
        EventResponse response = eventService.updateEvent(UUID.fromString(userId), id, request);
        return ResponseEntity.ok(ApiResponse.success("Event updated.", response));
    }

    @PostMapping("/{id}/merch")
    @PreAuthorize("hasRole('ORGANIZER')")
    @Operation(summary = "Attach a merch item to an event")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Merch attached to event"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed — see data for field errors"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized — missing or invalid JWT"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — ORGANIZER role required or resource belongs to another organization"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Event or merch item not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Merch already attached to this event"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<ApiResponse<EventResponse>> attachMerch(
        @PathVariable UUID id,
        @Valid @RequestBody AttachMerchRequest request,
        @RequestAttribute("userId") String userId
    ) {
        EventResponse response = eventService.attachMerch(UUID.fromString(userId), id, request.getMerchId());
        return ResponseEntity.ok(ApiResponse.success("Merch attached to event.", response));
    }

    @DeleteMapping("/{id}/merch/{merchId}")
    @PreAuthorize("hasRole('ORGANIZER')")
    @Operation(summary = "Detach a merch item from an event")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Merch detached from event"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized — missing or invalid JWT"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — ORGANIZER role required or resource belongs to another organization"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Event or merch item not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<Void> detachMerch(
        @PathVariable UUID id,
        @PathVariable UUID merchId,
        @RequestAttribute("userId") String userId
    ) {
        eventService.detachMerch(UUID.fromString(userId), id, merchId);
        return ResponseEntity.noContent().build();
    }
}
