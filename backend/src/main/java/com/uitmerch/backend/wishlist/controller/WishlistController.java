package com.uitmerch.backend.wishlist.controller;

import com.uitmerch.backend.common.model.ApiResponse;
import com.uitmerch.backend.wishlist.dto.WishlistResponse;
import com.uitmerch.backend.wishlist.service.WishlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customer/wishlist")
@RequiredArgsConstructor
@Tag(name = "Customer — Wishlist", description = "Save and manage favourite merch items")
@SecurityRequirement(name = "bearerAuth")
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get wishlist", description = "Returns all saved merch items with full merch details.")
    public ResponseEntity<ApiResponse<WishlistResponse>> getWishlist(
        @RequestAttribute("userId") String userId
    ) {
        return ResponseEntity.ok(
            ApiResponse.success("Wishlist retrieved.", wishlistService.getWishlist(UUID.fromString(userId)))
        );
    }

    @PostMapping("/{merchId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Add item to wishlist")
    public ResponseEntity<ApiResponse<WishlistResponse>> addItem(
        @PathVariable UUID merchId,
        @RequestAttribute("userId") String userId
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
            ApiResponse.success("Item added to wishlist.", wishlistService.addItem(UUID.fromString(userId), merchId))
        );
    }

    @DeleteMapping("/{merchId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Remove item from wishlist")
    public ResponseEntity<Void> removeItem(
        @PathVariable UUID merchId,
        @RequestAttribute("userId") String userId
    ) {
        wishlistService.removeItem(UUID.fromString(userId), merchId);
        return ResponseEntity.noContent().build();
    }
}
