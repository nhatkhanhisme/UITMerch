package com.uitmerch.backend.ai.controller;

import com.uitmerch.backend.ai.dto.VisualSearchResponse;
import com.uitmerch.backend.ai.service.VisualSearchService;
import com.uitmerch.backend.common.model.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/public/merch")
@RequiredArgsConstructor
@Tag(name = "Public", description = "Browse published merchandise")
public class AiMerchSearchController {

    private final VisualSearchService visualSearchService;

    @PostMapping(value = "/visual-search", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Visual merch search",
        description = "Upload an image containing a merch item; AI describes it and returns relevant results. " +
                      "Accepts JPEG, PNG, WebP up to 5 MB. No authentication required."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Search completed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid file type, size, or AI error")
    })
    public ResponseEntity<ApiResponse<VisualSearchResponse>> visualSearch(
        @RequestPart("image") MultipartFile image
    ) {
        VisualSearchResponse result = visualSearchService.search(image);
        return ResponseEntity.ok(ApiResponse.success("Tìm kiếm thành công.", result));
    }
}
