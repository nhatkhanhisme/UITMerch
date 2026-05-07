package com.uitmerch.backend.catalog.controller;

import com.uitmerch.backend.catalog.dto.MerchItemResponse;
import com.uitmerch.backend.catalog.dto.SearchMerchRequest;
import com.uitmerch.backend.catalog.service.MerchService;
import com.uitmerch.backend.common.model.ApiResponse;
import com.uitmerch.backend.common.model.PaginationMeta;
import com.uitmerch.backend.common.util.TraceIdUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public merch catalog endpoints.
 */
@RestController
@RequestMapping("/api/v1/public/merch")
@Tag(name = "4. Public Catalog", description = "Public merch catalog search and discovery")
public class PublicMerchController {

    private final MerchService merchService;

    public PublicMerchController(MerchService merchService) {
        this.merchService = merchService;
    }

    @GetMapping
    @Operation(
        summary = "Search merch catalog",
        description = "Searches public merch catalog with pagination, keyword search, organization filter, and pre-order filter. Returns paginated results with metadata.",
        security = {} // No security required for this public endpoint
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Search completed successfully", content = @Content(schema = @Schema(implementation = MerchItemResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid pagination or filter parameters")
    })
    public ResponseEntity<ApiResponse<List<MerchItemResponse>>> searchMerch(
        @Valid @ModelAttribute SearchMerchRequest request
    ) {
        Page<MerchItemResponse> merchPage = merchService.searchPublicMerch(request);

        PaginationMeta meta = PaginationMeta.builder()
            .page(merchPage.getNumber())
            .pageSize(merchPage.getSize())
            .totalElements(merchPage.getTotalElements())
            .totalPages(merchPage.getTotalPages())
            .hasNext(merchPage.hasNext())
            .hasPrevious(merchPage.hasPrevious())
            .build();

        return ResponseEntity.ok(ApiResponse.successWithMeta(merchPage.getContent(), meta, TraceIdUtil.getOrCreateTraceId()));
    }
}