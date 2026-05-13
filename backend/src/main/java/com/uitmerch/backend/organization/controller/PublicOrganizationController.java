package com.uitmerch.backend.organization.controller;

import com.uitmerch.backend.common.model.ApiResponse;
import com.uitmerch.backend.common.model.PaginationMeta;
import com.uitmerch.backend.event.dto.EventResponse;
import com.uitmerch.backend.event.service.EventService;
import com.uitmerch.backend.merch.dto.MerchResponse;
import com.uitmerch.backend.merch.service.MerchService;
import com.uitmerch.backend.organization.dto.OrganizationResponse;
import com.uitmerch.backend.organization.service.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/public/organizations")
@RequiredArgsConstructor
@Tag(name = "Public", description = "Browse active organizations")
public class PublicOrganizationController {

    private final OrganizationService organizationService;
    private final MerchService merchService;
    private final EventService eventService;

    @GetMapping
    @Operation(summary = "List active organizations", description = "Returns all ACTIVE organizations. Supports pagination.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Organizations retrieved"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<ApiResponse<java.util.List<OrganizationResponse>>> listOrganizations(
        @ParameterObject @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<OrganizationResponse> page = organizationService.listActiveOrganizations(pageable);
        return ResponseEntity.ok(
            ApiResponse.success("Organizations retrieved.", page.getContent(), PaginationMeta.from(page))
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get organization by ID")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Organization retrieved"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Organization not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<ApiResponse<OrganizationResponse>> getOrganization(@PathVariable UUID id) {
        OrganizationResponse response = organizationService.getOrganization(id);
        return ResponseEntity.ok(ApiResponse.success("Organization retrieved.", response));
    }

    @GetMapping("/{id}/merch")
    @Operation(summary = "List published merch for an organization")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Merch items retrieved"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Organization not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<ApiResponse<java.util.List<MerchResponse>>> getOrgMerch(
        @PathVariable UUID id,
        @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<MerchResponse> page = merchService.listByOrganization(id, pageable);
        return ResponseEntity.ok(ApiResponse.success("Merch items retrieved.", page.getContent(), PaginationMeta.from(page)));
    }

    @GetMapping("/{id}/events")
    @Operation(summary = "List published events for an organization")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Events retrieved"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Organization not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<ApiResponse<java.util.List<EventResponse>>> getOrgEvents(
        @PathVariable UUID id,
        @ParameterObject @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<EventResponse> page = eventService.getPublicEventsByOrg(id, pageable);
        return ResponseEntity.ok(ApiResponse.success("Events retrieved.", page.getContent(), PaginationMeta.from(page)));
    }
}
