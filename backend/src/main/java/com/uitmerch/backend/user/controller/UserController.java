package com.uitmerch.backend.user.controller;

import com.uitmerch.backend.common.model.ApiResponse;
import com.uitmerch.backend.user.dto.UpdateProfileRequest;
import com.uitmerch.backend.user.dto.UserProfileResponse;
import com.uitmerch.backend.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customer")
@RequiredArgsConstructor
@Tag(name = "Customer — Profile", description = "Manage own profile")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get own profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(
        @RequestAttribute("userId") String userId
    ) {
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved.", userService.getProfile(UUID.fromString(userId))));
    }

    @PatchMapping("/profile")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Update own profile", description = "Only provided fields are updated.")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
        @RequestBody UpdateProfileRequest request,
        @RequestAttribute("userId") String userId
    ) {
        return ResponseEntity.ok(ApiResponse.success("Profile updated.", userService.updateProfile(UUID.fromString(userId), request)));
    }
}
