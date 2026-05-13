package com.uitmerch.backend.organization.controller;

import com.uitmerch.backend.common.model.ApiResponse;
import com.uitmerch.backend.organization.dto.CreateOrganizationRequest;
import com.uitmerch.backend.organization.dto.OrganizationResponse;
import com.uitmerch.backend.organization.dto.UpdateOrganizationRequest;
import com.uitmerch.backend.organization.service.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations")
@RequiredArgsConstructor
@Tag(name = "Organizer", description = "Manage own organization profile")
@SecurityRequirement(name = "bearerAuth")
public class OrganizationController {

    private final OrganizationService organizationService;

    @PostMapping("/me")
    @PreAuthorize("hasRole('ORGANIZER')")
    @Operation(summary = "Create organization", description = "Creates a new organization profile with PENDING status. One per organizer.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Organization created and pending admin approval"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed — see data for field errors"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized — missing or invalid JWT"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — ORGANIZER role required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Organizer already has an organization"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<ApiResponse<OrganizationResponse>> createOrganization(
        @Valid @RequestBody CreateOrganizationRequest request,
        @RequestAttribute("userId") String userId
    ) {
        OrganizationResponse response = organizationService.createOrganization(
            UUID.fromString(userId), request
        );
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success("Organization created and pending admin approval.", response));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('ORGANIZER')")
    @Operation(summary = "Get own organization")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Organization retrieved"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized — missing or invalid JWT"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — ORGANIZER role required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Organization not found for this user"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<ApiResponse<OrganizationResponse>> getOwnOrganization(
        @RequestAttribute("userId") String userId
    ) {
        OrganizationResponse response = organizationService.getOwnOrganization(UUID.fromString(userId));
        return ResponseEntity.ok(ApiResponse.success("Organization retrieved.", response));
    }

    @PatchMapping("/me")
    @PreAuthorize("hasRole('ORGANIZER')")
    @Operation(summary = "Update own organization", description = "Updates name, description, logoUrl, or coverUrl. Only provided fields are changed.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Organization updated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed — see data for field errors"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized — missing or invalid JWT"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — ORGANIZER role required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Organization not found for this user"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<ApiResponse<OrganizationResponse>> updateOrganization(
        @RequestBody UpdateOrganizationRequest request,
        @RequestAttribute("userId") String userId
    ) {
        OrganizationResponse response = organizationService.updateOrganization(
            UUID.fromString(userId), request
        );
        return ResponseEntity.ok(ApiResponse.success("Organization updated.", response));
    }
}
