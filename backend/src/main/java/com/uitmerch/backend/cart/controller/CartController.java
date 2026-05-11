package com.uitmerch.backend.cart.controller;

import com.uitmerch.backend.cart.dto.*;
import com.uitmerch.backend.cart.service.CartService;
import com.uitmerch.backend.common.model.ApiResponse;
import com.uitmerch.backend.order.dto.OrderResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customer/cart")
@RequiredArgsConstructor
@Tag(name = "Customer — Cart", description = "Manage shopping cart for authenticated customers")
@SecurityRequirement(name = "bearerAuth")
public class CartController {

    private final CartService cartService;

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get current cart", description = "Returns the active cart for the authenticated customer.")
    public ResponseEntity<ApiResponse<CartResponse>> getCart(
        @RequestAttribute("userId") String userId
    ) {
        CartResponse cart = cartService.getCart(UUID.fromString(userId));
        return ResponseEntity.ok(ApiResponse.success("Cart retrieved.", cart));
    }

    @PostMapping("/items")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Add item to cart", description = "Adds a merch item to the cart. Returns 409 if item already exists — use PATCH to update quantity.")
    public ResponseEntity<ApiResponse<CartResponse>> addItem(
        @Valid @RequestBody AddCartItemRequest request,
        @RequestAttribute("userId") String userId
    ) {
        CartResponse cart = cartService.addItem(UUID.fromString(userId), request);
        return ResponseEntity.ok(ApiResponse.success("Item added to cart.", cart));
    }

    @PatchMapping("/items/{itemId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Update cart item quantity")
    public ResponseEntity<ApiResponse<CartResponse>> updateItem(
        @PathVariable UUID itemId,
        @Valid @RequestBody UpdateCartItemRequest request,
        @RequestAttribute("userId") String userId
    ) {
        CartResponse cart = cartService.updateItem(UUID.fromString(userId), itemId, request);
        return ResponseEntity.ok(ApiResponse.success("Cart item updated.", cart));
    }

    @DeleteMapping("/items/{itemId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Remove item from cart")
    public ResponseEntity<Void> removeItem(
        @PathVariable UUID itemId,
        @RequestAttribute("userId") String userId
    ) {
        cartService.removeItem(UUID.fromString(userId), itemId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/checkout")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Checkout cart", description = "Converts the active cart into one or more orders grouped by organization.")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> checkout(
        @RequestBody(required = false) CheckoutRequest request,
        @RequestAttribute("userId") String userId
    ) {
        if (request == null) {
            request = new CheckoutRequest();
        }
        List<OrderResponse> orders = cartService.checkout(UUID.fromString(userId), request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Checkout successful. " + orders.size() + " order(s) created.", orders));
    }
}
