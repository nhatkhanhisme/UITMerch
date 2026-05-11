package com.uitmerch.backend.merch.controller;

import com.uitmerch.backend.common.model.ApiResponse;
import com.uitmerch.backend.common.model.PaginationMeta;
import com.uitmerch.backend.merch.dto.MerchResponse;
import com.uitmerch.backend.merch.service.MerchService;
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
@RequestMapping("/api/v1/public/merch")
@RequiredArgsConstructor
@Tag(name = "Public — Merch", description = "Browse published merchandise")
public class PublicMerchController {

    private final MerchService merchService;

    @GetMapping
    @Operation(summary = "List published merch", description = "Supports optional ?keyword= filter and pagination.")
    public ResponseEntity<ApiResponse<List<MerchResponse>>> listMerch(
        @RequestParam(required = false) String keyword,
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<MerchResponse> page = merchService.listPublished(keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success("Merch items retrieved.", page.getContent(), PaginationMeta.from(page)));
    }

    @GetMapping("/popular")
    @Operation(summary = "Get popular merch", description = "Returns up to 10 recently published items.")
    public ResponseEntity<ApiResponse<List<MerchResponse>>> getPopular() {
        return ResponseEntity.ok(ApiResponse.success("Popular merch retrieved.", merchService.getPopularMerch()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get merch item by ID")
    public ResponseEntity<ApiResponse<MerchResponse>> getMerch(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Merch item retrieved.", merchService.getPublishedMerch(id)));
    }
}
