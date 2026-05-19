package com.uitmerch.backend.cart.service;

import com.uitmerch.backend.cart.dto.*;
import com.uitmerch.backend.cart.entity.Cart;
import com.uitmerch.backend.cart.entity.CartItem;
import com.uitmerch.backend.cart.repository.CartItemRepository;
import com.uitmerch.backend.cart.repository.CartRepository;
import com.uitmerch.backend.common.exception.ConflictException;
import com.uitmerch.backend.common.exception.ResourceNotFoundException;
import com.uitmerch.backend.common.exception.ValidationException;
import com.uitmerch.backend.common.model.CartStatus;
import com.uitmerch.backend.merch.dto.MerchResponse;
import com.uitmerch.backend.merch.entity.MerchImage;
import com.uitmerch.backend.merch.entity.MerchItem;
import com.uitmerch.backend.merch.repository.MerchImageRepository;
import com.uitmerch.backend.merch.repository.MerchItemRepository;
import com.uitmerch.backend.order.dto.OrderResponse;
import com.uitmerch.backend.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final MerchItemRepository merchItemRepository;
    private final MerchImageRepository merchImageRepository;
    private final OrderService orderService;

    // ------------------------------------------------------------------ //
    //  GET CART
    // ------------------------------------------------------------------ //

    @Transactional(readOnly = true)
    public CartResponse getCart(UUID userId) {
        Cart cart = findOrCreateActiveCart(userId);
        return buildCartResponse(cart);
    }

    // ------------------------------------------------------------------ //
    //  ADD ITEM
    // ------------------------------------------------------------------ //

    @Transactional
    public CartResponse addItem(UUID userId, AddCartItemRequest request) {
        Cart cart = findOrCreateActiveCart(userId);

        if (cartItemRepository.existsByCartIdAndMerchId(cart.getId(), request.getMerchId())) {
            throw new ConflictException(
                "This item is already in your cart. Use PATCH /items/{itemId} to update the quantity."
            );
        }

        MerchItem merch = merchItemRepository.findById(request.getMerchId())
            .orElseThrow(() -> new ResourceNotFoundException("Merch item", request.getMerchId().toString()));

        if (merch.getStock() <= 0) {
            throw new ValidationException("This item is out of stock.");
        }

        CartItem item = CartItem.builder()
            .cartId(cart.getId())
            .merchId(request.getMerchId())
            .quantity(request.getQuantity())
            .build();
        cartItemRepository.save(item);

        return buildCartResponse(cart);
    }

    // ------------------------------------------------------------------ //
    //  UPDATE ITEM
    // ------------------------------------------------------------------ //

    @Transactional
    public CartResponse updateItem(UUID userId, UUID itemId, UpdateCartItemRequest request) {
        Cart cart = getActiveCartOrThrow(userId);

        CartItem item = cartItemRepository.findById(itemId)
            .orElseThrow(() -> new ResourceNotFoundException("Cart item", itemId.toString()));

        if (!cart.getId().equals(item.getCartId())) {
            throw new ResourceNotFoundException("Cart item", itemId.toString());
        }

        MerchItem merch = merchItemRepository.findById(item.getMerchId())
            .orElseThrow(() -> new ResourceNotFoundException("Merch item", item.getMerchId().toString()));

        if (merch.getStock() < request.getQuantity()) {
            throw new ValidationException(
                "Insufficient stock for \"" + merch.getName() + "\". Available: " + merch.getStock()
            );
        }

        item.setQuantity(request.getQuantity());
        cartItemRepository.save(item);

        return buildCartResponse(cart);
    }

    // ------------------------------------------------------------------ //
    //  REMOVE ITEM
    // ------------------------------------------------------------------ //

    @Transactional
    public void removeItem(UUID userId, UUID itemId) {
        Cart cart = getActiveCartOrThrow(userId);

        CartItem item = cartItemRepository.findById(itemId)
            .orElseThrow(() -> new ResourceNotFoundException("Cart item", itemId.toString()));

        if (!cart.getId().equals(item.getCartId())) {
            throw new ResourceNotFoundException("Cart item", itemId.toString());
        }

        cartItemRepository.delete(item);
    }

    // ------------------------------------------------------------------ //
    //  CHECKOUT
    // ------------------------------------------------------------------ //

    @Transactional
    public List<OrderResponse> checkout(UUID userId, CheckoutRequest request) {
        Cart cart = getActiveCartOrThrow(userId);
        List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId());

        if (cartItems.isEmpty()) {
            throw new ValidationException("Your cart is empty. Add items before checking out.");
        }

        List<OrderResponse> orders = orderService.createOrdersFromCart(
            userId, cart, cartItems, request.getNote(),
            request.getShippingName(), request.getShippingPhone(), request.getShippingAddress()
        );

        cartItemRepository.deleteAll(cartItems);
        cart.setStatus(CartStatus.CHECKED_OUT);
        cartRepository.save(cart);

        return orders;
    }

    // ------------------------------------------------------------------ //
    //  HELPERS
    // ------------------------------------------------------------------ //

    private Cart findOrCreateActiveCart(UUID userId) {
        return cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)
            .orElseGet(() -> {
                List<Cart> existing = cartRepository.findByUserId(userId);
                if (!existing.isEmpty()) {
                    Cart cart = existing.get(0);
                    cart.setStatus(CartStatus.ACTIVE);
                    return cartRepository.save(cart);
                }
                return cartRepository.save(Cart.builder().userId(userId).build());
            });
    }

    private Cart getActiveCartOrThrow(UUID userId) {
        return cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)
            .orElseThrow(() -> new ResourceNotFoundException("Active cart not found for this user."));
    }

    private CartResponse buildCartResponse(Cart cart) {
        List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId());

        List<UUID> merchIds = cartItems.stream().map(CartItem::getMerchId).toList();
        Map<UUID, MerchItem> merchMap = merchItemRepository.findAllById(merchIds)
            .stream().collect(Collectors.toMap(MerchItem::getId, Function.identity()));
        Map<UUID, List<String>> imageMap = merchImageRepository
            .findByMerchIdInOrderByPosition(merchIds)
            .stream()
            .collect(Collectors.groupingBy(
                MerchImage::getMerchId,
                Collectors.mapping(MerchImage::getUrl, Collectors.toList())
            ));

        List<CartItemResponse> itemResponses = cartItems.stream()
            .map(item -> {
                MerchItem merch = merchMap.get(item.getMerchId());
                BigDecimal unitPrice = (merch != null && merch.getPrice() != null) ? merch.getPrice() : BigDecimal.ZERO;
                BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));

                return CartItemResponse.builder()
                    .id(item.getId())
                    .cartId(item.getCartId())
                    .merch(merch != null
                        ? MerchResponse.from(merch, null, imageMap.getOrDefault(merch.getId(), List.of()))
                        : null)
                    .quantity(item.getQuantity())
                    .subtotal(subtotal)
                    .createdAt(item.getCreatedAt())
                    .build();
            })
            .toList();

        BigDecimal totalAmount = itemResponses.stream()
            .map(CartItemResponse::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
            .id(cart.getId())
            .userId(cart.getUserId())
            .status(cart.getStatus())
            .items(itemResponses)
            .totalAmount(totalAmount)
            .createdAt(cart.getCreatedAt())
            .updatedAt(cart.getUpdatedAt())
            .build();
    }
}
