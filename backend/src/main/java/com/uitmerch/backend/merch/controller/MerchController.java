package com.uitmerch.backend.merch.controller;

import com.uitmerch.backend.common.model.ApiResponse;
import com.uitmerch.backend.common.model.PaginationMeta;
import com.uitmerch.backend.merch.dto.CreateMerchRequest;
import com.uitmerch.backend.merch.dto.MerchResponse;
import com.uitmerch.backend.merch.dto.UpdateMerchRequest;
import com.uitmerch.backend.merch.service.MerchService;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations/merchs")
@RequiredArgsConstructor
@Tag(name = "Organizer", description = "Manage merch catalog for own organization")
@SecurityRequirement(name = "bearerAuth")
public class MerchController {

    private final MerchService merchService;

    @PostMapping
    @PreAuthorize("hasRole('ORGANIZER')")
    @Operation(summary = "Create merch item", description = "Organization must be ACTIVE (BR08). Pass imageUrls as an ordered list of image URLs.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Merch item created"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed — see data for field errors or organization not ACTIVE"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized — missing or invalid JWT"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — ORGANIZER role required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Organization not found for this user"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<ApiResponse<MerchResponse>> createMerch(
        @Valid @RequestBody CreateMerchRequest request,
        @RequestAttribute("userId") String userId
    ) {
        MerchResponse response = merchService.createMerch(UUID.fromString(userId), request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Merch item created.", response));
    }

    @GetMapping
    @PreAuthorize("hasRole('ORGANIZER')")
    @Operation(summary = "List own merch items", description = "Returns all statuses (DRAFT, PUBLISHED, ARCHIVED).")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Merch items retrieved"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized — missing or invalid JWT"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — ORGANIZER role required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<ApiResponse<java.util.List<MerchResponse>>> getOwnMerch(
        @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
        @RequestAttribute("userId") String userId
    ) {
        Page<MerchResponse> page = merchService.getOwnMerch(UUID.fromString(userId), pageable);
        return ResponseEntity.ok(ApiResponse.success("Merch items retrieved.", page.getContent(), PaginationMeta.from(page)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZER')")
    @Operation(summary = "Get own merch item by ID")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Merch item retrieved"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized — missing or invalid JWT"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — ORGANIZER role required or item belongs to another organization"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Merch item not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<ApiResponse<MerchResponse>> getOwnMerchItem(
        @PathVariable UUID id,
        @RequestAttribute("userId") String userId
    ) {
        MerchResponse response = merchService.getOwnMerchItem(UUID.fromString(userId), id);
        return ResponseEntity.ok(ApiResponse.success("Merch item retrieved.", response));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZER')")
    @Operation(summary = "Update merch item", description = "Supports partial update including status change. Pass imageUrls to replace all images; omit to keep existing images; pass an empty list to remove all.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Merch item updated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed — see data for field errors"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized — missing or invalid JWT"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — ORGANIZER role required or item belongs to another organization"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Merch item not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<ApiResponse<MerchResponse>> updateMerch(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateMerchRequest request,
        @RequestAttribute("userId") String userId
    ) {
        MerchResponse response = merchService.updateMerch(UUID.fromString(userId), id, request);
        return ResponseEntity.ok(ApiResponse.success("Merch item updated.", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZER')")
    @Operation(summary = "Soft-delete merch item", description = "Sets status to ARCHIVED (BR06).")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Merch item archived"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized — missing or invalid JWT"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — ORGANIZER role required or item belongs to another organization"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Merch item not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<ApiResponse<Void>> deleteMerch(
        @PathVariable UUID id,
        @RequestAttribute("userId") String userId
    ) {
        merchService.deleteMerch(UUID.fromString(userId), id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
