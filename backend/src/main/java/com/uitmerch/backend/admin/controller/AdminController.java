package com.uitmerch.backend.admin.controller;

import com.uitmerch.backend.admin.dto.UpdateOrganizationStatusRequest;
import com.uitmerch.backend.admin.dto.UpdateUserRoleRequest;
import com.uitmerch.backend.admin.dto.UserSummaryResponse;
import com.uitmerch.backend.admin.service.AdminService;
import com.uitmerch.backend.common.model.ApiResponse;
import com.uitmerch.backend.common.model.PaginationMeta;
import com.uitmerch.backend.order.dto.OrderResponse;
import com.uitmerch.backend.organization.dto.OrganizationResponse;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Platform administration endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all users", description = "Supports optional role filter. Paginated.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Users retrieved"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized — missing or invalid JWT"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — ADMIN role required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<ApiResponse<List<UserSummaryResponse>>> listUsers(
        @RequestParam(required = false) String role,
        @ParameterObject @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<UserSummaryResponse> page = adminService.listUsers(role, pageable);
        return ResponseEntity.ok(
            ApiResponse.success("Users retrieved.", page.getContent(), PaginationMeta.from(page))
        );
    }

    @PatchMapping("/users/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a user's role")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User role updated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed — see data for field errors"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized — missing or invalid JWT"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — ADMIN role required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<ApiResponse<UserSummaryResponse>> updateUserRole(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateUserRoleRequest request
    ) {
        UserSummaryResponse response = adminService.updateUserRole(id, request.getRole());
        return ResponseEntity.ok(ApiResponse.success("User role updated.", response));
    }

    @PatchMapping("/users/{id}/active")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activate or deactivate a user account")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User status updated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN role required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<ApiResponse<UserSummaryResponse>> setUserActive(
        @PathVariable UUID id,
        @RequestParam boolean active
    ) {
        UserSummaryResponse response = adminService.setUserActive(id, active);
        return ResponseEntity.ok(ApiResponse.success(
            active ? "User activated." : "User deactivated.", response
        ));
    }

    @GetMapping("/organizations")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all organizations", description = "Supports optional status filter. Paginated.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Organizations retrieved"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized — missing or invalid JWT"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — ADMIN role required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<ApiResponse<List<OrganizationResponse>>> listOrganizations(
        @RequestParam(required = false) String status,
        @ParameterObject @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<OrganizationResponse> page = adminService.listOrganizations(status, pageable);
        return ResponseEntity.ok(
            ApiResponse.success("Organizations retrieved.", page.getContent(), PaginationMeta.from(page))
        );
    }

    @PatchMapping("/organizations/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update an organization's status")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Organization status updated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed — see data for field errors"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized — missing or invalid JWT"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — ADMIN role required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Organization not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<ApiResponse<OrganizationResponse>> updateOrganizationStatus(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateOrganizationStatusRequest request
    ) {
        OrganizationResponse response = adminService.updateOrganizationStatus(id, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success("Organization status updated.", response));
    }

    @GetMapping("/orders")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all orders", description = "Supports optional status filter. Paginated.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Orders retrieved"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized — missing or invalid JWT"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — ADMIN role required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<ApiResponse<List<OrderResponse>>> listAllOrders(
        @RequestParam(required = false) String status,
        @ParameterObject @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<OrderResponse> page = adminService.listAllOrders(status, pageable);
        return ResponseEntity.ok(
            ApiResponse.success("Orders retrieved.", page.getContent(), PaginationMeta.from(page))
        );
    }
}
