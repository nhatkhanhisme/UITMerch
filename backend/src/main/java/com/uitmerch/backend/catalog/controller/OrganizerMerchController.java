package com.uitmerch.backend.catalog.controller;

import com.uitmerch.backend.catalog.dto.CreateMerchRequest;
import com.uitmerch.backend.catalog.dto.MerchItemResponse;
import com.uitmerch.backend.catalog.service.MerchService;
import com.uitmerch.backend.common.model.ApiResponse;
import com.uitmerch.backend.common.util.TraceIdUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Organizer merch catalog endpoints.
 */
@RestController
@RequestMapping("/api/v1/organizer/merch")
@PreAuthorize("hasRole('ORGANIZER')")
@Tag(name = "3. Organizer Catalog", description = "Organizer merch item creation and management")
public class OrganizerMerchController {

    private final MerchService merchService;

    public OrganizerMerchController(MerchService merchService) {
        this.merchService = merchService;
    }

    @PostMapping
    @Operation(summary = "Create merch item", description = "Creates a new merch item for the authenticated organizer's organization. Supports both ready-stock (with stockQuantity) and pre-order (isPreorder=true) items.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Merch item created successfully", content = @Content(schema = @Schema(implementation = MerchItemResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request data or business rule violation"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - must have ORGANIZER role")
    })
    public ResponseEntity<ApiResponse<MerchItemResponse>> createMerch(
        @Valid @RequestBody CreateMerchRequest request
    ) {
        MerchItemResponse response = merchService.createMerch(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(response, TraceIdUtil.getOrCreateTraceId()));
    }
}