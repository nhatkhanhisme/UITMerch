package com.uitmerch.backend.merch.controller;

import com.uitmerch.backend.common.model.ApiResponse;
import com.uitmerch.backend.common.model.PaginationMeta;
import com.uitmerch.backend.merch.dto.MerchResponse;
import com.uitmerch.backend.merch.service.MerchService;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/public/merch")
@RequiredArgsConstructor
@Tag(name = "Public — Merch", description = "Browse published merchandise")
public class PublicMerchController {

    private final MerchService merchService;

    @GetMapping
    @Operation(summary = "List published merch", description = "Supports optional ?keyword= and ?category= (slug) filters with pagination.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Merch items retrieved"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Category slug not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<ApiResponse<List<MerchResponse>>> listMerch(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String category,
        @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<MerchResponse> page = merchService.listPublished(keyword, category, pageable);
        return ResponseEntity.ok(ApiResponse.success("Merch items retrieved.", page.getContent(), PaginationMeta.from(page)));
    }

    @GetMapping("/popular")
    @Operation(summary = "Get popular merch", description = "Returns up to 10 recently published items.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Popular merch retrieved"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<ApiResponse<List<MerchResponse>>> getPopular() {
        return ResponseEntity.ok(ApiResponse.success("Popular merch retrieved.", merchService.getPopularMerch()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get merch item by ID")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Merch item retrieved"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Merch item not found or not published"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<ApiResponse<MerchResponse>> getMerch(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Merch item retrieved.", merchService.getPublishedMerch(id)));
    }
}
