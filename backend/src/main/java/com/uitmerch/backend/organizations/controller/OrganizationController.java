package com.uitmerch.backend.organizations.controller;

import com.uitmerch.backend.common.model.ApiResponse;
import com.uitmerch.backend.common.util.TraceIdUtil;
import com.uitmerch.backend.organizations.dto.OrganizationResponse;
import com.uitmerch.backend.organizations.dto.OrganizationUpdateRequest;
import com.uitmerch.backend.organizations.service.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Organizer organization management endpoints.
 */
@RestController
@RequestMapping("/api/v1/organizer/organizations")
@PreAuthorize("hasRole('ORGANIZER')")
@Tag(name = "2. Organizer Profile", description = "Organizer organization profile management")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get current organizer's organization", description = "Retrieves the organization profile for the authenticated organizer.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Organization retrieved successfully", content = @Content(schema = @Schema(implementation = OrganizationResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - must have ORGANIZER role"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Organization not found for authenticated user")
    })
    public ResponseEntity<ApiResponse<OrganizationResponse>> getMyOrganization() {
        OrganizationResponse response = organizationService.getMyOrganization();
        return ResponseEntity.ok(ApiResponse.success(response, TraceIdUtil.getOrCreateTraceId()));
    }

    @PatchMapping("/{organizationId}")
    @Operation(summary = "Update organization profile", description = "Updates organization details. Only the organization owner can update their organization.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Organization updated successfully", content = @Content(schema = @Schema(implementation = OrganizationResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request data"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - must be organization owner"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Organization not found")
    })
    public ResponseEntity<ApiResponse<OrganizationResponse>> updateOrganization(
        @PathVariable UUID organizationId,
        @Valid @RequestBody OrganizationUpdateRequest request
    ) {
        OrganizationResponse response = organizationService.updateOrganization(organizationId, request);
        return ResponseEntity.ok(ApiResponse.success(response, TraceIdUtil.getOrCreateTraceId()));
    }
}