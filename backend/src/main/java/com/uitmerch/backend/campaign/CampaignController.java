package com.uitmerch.backend.campaign;
import com.uitmerch.backend.common.model.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
@RestController @RequestMapping("/api/v1") @RequiredArgsConstructor
public class CampaignController {
    private final CampaignService service;
    @PostMapping("/organizations/{orgId}/campaigns") @PreAuthorize("hasRole('ORGANIZER')")
    @Operation(summary="Create a preorder campaign with 1-20 published SKUs; reserve stock until its deadline")
    public ApiResponse<CampaignResponse> create(@RequestAttribute("userId") String user,@PathVariable UUID orgId,@Valid @RequestBody CampaignRequests.Create request) {
        return ApiResponse.success("Campaign created.",service.create(UUID.fromString(user),orgId,request));
    }
    @GetMapping("/organizations/{orgId}/campaigns") @PreAuthorize("hasRole('ORGANIZER')")
    public ApiResponse<Page<CampaignResponse>> own(@RequestAttribute("userId") String user,@PathVariable UUID orgId,Pageable page) {
        return ApiResponse.success("Campaigns.",service.organizationList(UUID.fromString(user),orgId,page));
    }
    @PostMapping("/organizations/{orgId}/campaigns/{id}/cancel") @PreAuthorize("hasRole('ORGANIZER')")
    public ApiResponse<CampaignResponse> cancel(@RequestAttribute("userId") String user,@PathVariable UUID orgId,@PathVariable UUID id) {
        return ApiResponse.success("Campaign closed.",service.cancel(UUID.fromString(user),orgId,id));
    }
    @GetMapping("/public/campaigns")
    public ApiResponse<Page<CampaignResponse>> list(Pageable page) { return ApiResponse.success("Campaigns.",service.publicList(page)); }
    @GetMapping("/public/campaigns/{id}")
    public ApiResponse<CampaignResponse> detail(@PathVariable UUID id) { return ApiResponse.success("Campaign.",service.publicDetail(id)); }
    @PostMapping("/customer/campaigns/{id}/reservations") @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary="Reserve a variant; replay with the same requestId returns the original order")
    public ApiResponse<CampaignService.ReservedOrder> reserve(@RequestAttribute("userId") String user,@PathVariable UUID id,@Valid @RequestBody CampaignRequests.Reserve request) {
        return ApiResponse.success("Reserved.",service.reserve(UUID.fromString(user),id,request));
    }
    @GetMapping("/customer/campaign-reservations") @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResponse<Page<CampaignService.ReservationResponse>> reservations(@RequestAttribute("userId") String user,Pageable page) {
        return ApiResponse.success("Reservations.",service.reservations(UUID.fromString(user),page));
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> conflict(DataIntegrityViolationException failure) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error("Reservation conflicted with another request. Retry with the same requestId."));
    }
}
