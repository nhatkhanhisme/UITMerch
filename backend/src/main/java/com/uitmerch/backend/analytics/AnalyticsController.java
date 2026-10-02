package com.uitmerch.backend.analytics;

import com.uitmerch.backend.common.model.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations/{orgId}/analytics")
@PreAuthorize("hasRole('ORGANIZER')")
@RequiredArgsConstructor
public class AnalyticsController {
    private final AnalyticsService service;
    @GetMapping
    @Operation(summary = "Organization analytics; current order status grouped by creation date, separate paid value")
    public ApiResponse<AnalyticsResponse> report(@RequestAttribute("userId") String user, @PathVariable UUID orgId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.success("Analytics.", service.report(UUID.fromString(user), orgId, from, to));
    }
}
