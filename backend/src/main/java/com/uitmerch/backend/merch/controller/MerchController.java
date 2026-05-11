package com.uitmerch.backend.merch.controller;

import com.uitmerch.backend.common.model.ApiResponse;
import com.uitmerch.backend.common.model.PaginationMeta;
import com.uitmerch.backend.merch.dto.CreateMerchRequest;
import com.uitmerch.backend.merch.dto.MerchResponse;
import com.uitmerch.backend.merch.dto.UpdateMerchRequest;
import com.uitmerch.backend.merch.service.MerchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
@Tag(name = "Organizer — Merch", description = "Manage merch catalog for own organization")
@SecurityRequirement(name = "bearerAuth")
public class MerchController {

    private final MerchService merchService;

    @PostMapping
    @PreAuthorize("hasRole('ORGANIZER')")
    @Operation(summary = "Create merch item", description = "Organization must be ACTIVE (BR08).")
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
    public ResponseEntity<ApiResponse<java.util.List<MerchResponse>>> getOwnMerch(
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
        @RequestAttribute("userId") String userId
    ) {
        Page<MerchResponse> page = merchService.getOwnMerch(UUID.fromString(userId), pageable);
        return ResponseEntity.ok(ApiResponse.success("Merch items retrieved.", page.getContent(), PaginationMeta.from(page)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZER')")
    @Operation(summary = "Get own merch item by ID")
    public ResponseEntity<ApiResponse<MerchResponse>> getOwnMerchItem(
        @PathVariable UUID id,
        @RequestAttribute("userId") String userId
    ) {
        MerchResponse response = merchService.getOwnMerchItem(UUID.fromString(userId), id);
        return ResponseEntity.ok(ApiResponse.success("Merch item retrieved.", response));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZER')")
    @Operation(summary = "Update merch item", description = "Supports partial update including status change.")
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
    public ResponseEntity<ApiResponse<Void>> deleteMerch(
        @PathVariable UUID id,
        @RequestAttribute("userId") String userId
    ) {
        merchService.deleteMerch(UUID.fromString(userId), id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
