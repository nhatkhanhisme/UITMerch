package com.uitmerch.backend.admin.controller;

import com.uitmerch.backend.admin.service.AdminService;
import com.uitmerch.backend.auth.dto.UpdateUserRoleRequest;
import com.uitmerch.backend.auth.dto.UserUpdateResponse;
import com.uitmerch.backend.common.model.ApiResponse;
import com.uitmerch.backend.common.util.TraceIdUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Admin endpoints for system governance and user management.
 * All endpoints require ADMIN role.
 * NFR02: Strict role checks at Controller/Route level using @PreAuthorize("hasRole('ADMIN')").
 */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "5. Admin Management", description = "Admin endpoints for user role management and system governance")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PatchMapping("/users/{userId}/role")
    @Operation(summary = "Update user role", description = "Promote or demote a user's role. Only ADMIN users can perform this action. User ID must be a valid UUID.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "User role updated successfully",
            content = @Content(schema = @Schema(implementation = UserUpdateResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request or invalid UUID format"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions (not an admin)"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<ApiResponse<UserUpdateResponse>> updateUserRole(
        @PathVariable UUID userId,
        @Valid @RequestBody UpdateUserRoleRequest request
    ) {
        UserUpdateResponse response = adminService.updateUserRole(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response, TraceIdUtil.getOrCreateTraceId()));
    }
}
