package com.uitmerch.backend.cart.service;

import com.uitmerch.backend.cart.dto.AddCartItemRequest;
import com.uitmerch.backend.cart.dto.CheckoutRequest;
import com.uitmerch.backend.cart.dto.UpdateCartItemRequest;
import com.uitmerch.backend.cart.entity.Cart;
import com.uitmerch.backend.cart.entity.CartItem;
import com.uitmerch.backend.cart.repository.CartItemRepository;
import com.uitmerch.backend.cart.repository.CartRepository;
import com.uitmerch.backend.common.exception.ConflictException;
import com.uitmerch.backend.common.exception.ResourceNotFoundException;
import com.uitmerch.backend.common.exception.ValidationException;
import com.uitmerch.backend.common.model.CartStatus;
import com.uitmerch.backend.merch.entity.MerchItem;
import com.uitmerch.backend.merch.repository.MerchItemRepository;
import com.uitmerch.backend.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private MerchItemRepository merchItemRepository;
    @Mock private OrderService orderService;

    @InjectMocks private CartService cartService;

    private final UUID userId = UUID.randomUUID();
    private final UUID cartId = UUID.randomUUID();
    private final UUID merchId = UUID.randomUUID();
    private final UUID itemId = UUID.randomUUID();

    private Cart activeCart() {
        return Cart.builder().id(cartId).userId(userId).status(CartStatus.ACTIVE).build();
    }

    private MerchItem merch(int stock) {
        return MerchItem.builder()
            .id(merchId)
            .name("Test Merch")
            .price(BigDecimal.valueOf(50_000))
            .stock(stock)
            .build();
    }

    // ── addItem ──────────────────────────────────────────────────────────────

    @Test
    void addItem_success_savesCartItem() {
        Cart cart = activeCart();
        when(cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
        when(cartItemRepository.existsByCartIdAndMerchId(cartId, merchId)).thenReturn(false);
        when(merchItemRepository.findById(merchId)).thenReturn(Optional.of(merch(10)));
        when(cartItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(cartItemRepository.findByCartId(cartId)).thenReturn(Collections.emptyList());
        when(merchItemRepository.findAllById(any())).thenReturn(Collections.emptyList());

        AddCartItemRequest req = new AddCartItemRequest();
        req.setMerchId(merchId);
        req.setQuantity(2);

        assertThatNoException().isThrownBy(() -> cartService.addItem(userId, req));
        verify(cartItemRepository).save(any());
    }

    @Test
    void addItem_duplicateItem_throwsConflict() {
        Cart cart = activeCart();
        when(cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
        when(cartItemRepository.existsByCartIdAndMerchId(cartId, merchId)).thenReturn(true);

        AddCartItemRequest req = new AddCartItemRequest();
        req.setMerchId(merchId);
        req.setQuantity(1);

        assertThatThrownBy(() -> cartService.addItem(userId, req))
            .isInstanceOf(ConflictException.class);
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void addItem_outOfStock_throwsValidation() {
        Cart cart = activeCart();
        when(cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
        when(cartItemRepository.existsByCartIdAndMerchId(cartId, merchId)).thenReturn(false);
        when(merchItemRepository.findById(merchId)).thenReturn(Optional.of(merch(0)));

        AddCartItemRequest req = new AddCartItemRequest();
        req.setMerchId(merchId);
        req.setQuantity(1);

        assertThatThrownBy(() -> cartService.addItem(userId, req))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("out of stock");
    }

    @Test
    void addItem_merchNotFound_throwsResourceNotFound() {
        Cart cart = activeCart();
        when(cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
        when(cartItemRepository.existsByCartIdAndMerchId(cartId, merchId)).thenReturn(false);
        when(merchItemRepository.findById(merchId)).thenReturn(Optional.empty());

        AddCartItemRequest req = new AddCartItemRequest();
        req.setMerchId(merchId);
        req.setQuantity(1);

        assertThatThrownBy(() -> cartService.addItem(userId, req))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void addItem_noActiveCart_createsNewCart() {
        Cart newCart = activeCart();
        when(cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)).thenReturn(Optional.empty());
        when(cartRepository.save(any())).thenReturn(newCart);
        when(cartItemRepository.existsByCartIdAndMerchId(cartId, merchId)).thenReturn(false);
        when(merchItemRepository.findById(merchId)).thenReturn(Optional.of(merch(5)));
        when(cartItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(cartItemRepository.findByCartId(cartId)).thenReturn(Collections.emptyList());
        when(merchItemRepository.findAllById(any())).thenReturn(Collections.emptyList());

        AddCartItemRequest req = new AddCartItemRequest();
        req.setMerchId(merchId);
        req.setQuantity(1);

        assertThatNoException().isThrownBy(() -> cartService.addItem(userId, req));
        verify(cartRepository).save(any());
    }

    // ── updateItem ───────────────────────────────────────────────────────────

    @Test
    void updateItem_success_updatesQuantity() {
        Cart cart = activeCart();
        CartItem item = CartItem.builder().id(itemId).cartId(cartId).merchId(merchId).quantity(1).build();
        when(cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(merchItemRepository.findById(merchId)).thenReturn(Optional.of(merch(10)));
        when(cartItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(cartItemRepository.findByCartId(cartId)).thenReturn(List.of(item));
        when(merchItemRepository.findAllById(any())).thenReturn(List.of(merch(10)));

        UpdateCartItemRequest req = new UpdateCartItemRequest();
        req.setQuantity(3);

        assertThatNoException().isThrownBy(() -> cartService.updateItem(userId, itemId, req));
        verify(cartItemRepository).save(eq(item));
    }

    @Test
    void updateItem_insufficientStock_throwsValidation() {
        Cart cart = activeCart();
        CartItem item = CartItem.builder().id(itemId).cartId(cartId).merchId(merchId).quantity(1).build();
        when(cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(merchItemRepository.findById(merchId)).thenReturn(Optional.of(merch(2)));

        UpdateCartItemRequest req = new UpdateCartItemRequest();
        req.setQuantity(5); // exceeds stock of 2

        assertThatThrownBy(() -> cartService.updateItem(userId, itemId, req))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Insufficient stock");
    }

    @Test
    void updateItem_itemBelongsToOtherCart_throwsResourceNotFound() {
        Cart cart = activeCart();
        UUID otherCartId = UUID.randomUUID();
        CartItem item = CartItem.builder().id(itemId).cartId(otherCartId).merchId(merchId).quantity(1).build();
        when(cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findById(itemId)).thenReturn(Optional.of(item));

        UpdateCartItemRequest req = new UpdateCartItemRequest();
        req.setQuantity(2);

        assertThatThrownBy(() -> cartService.updateItem(userId, itemId, req))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateItem_noActiveCart_throwsResourceNotFound() {
        when(cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)).thenReturn(Optional.empty());

        UpdateCartItemRequest req = new UpdateCartItemRequest();
        req.setQuantity(1);

        assertThatThrownBy(() -> cartService.updateItem(userId, itemId, req))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── removeItem ───────────────────────────────────────────────────────────

    @Test
    void removeItem_success_deletesItem() {
        Cart cart = activeCart();
        CartItem item = CartItem.builder().id(itemId).cartId(cartId).merchId(merchId).quantity(1).build();
        when(cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findById(itemId)).thenReturn(Optional.of(item));

        assertThatNoException().isThrownBy(() -> cartService.removeItem(userId, itemId));
        verify(cartItemRepository).delete(item);
    }

    @Test
    void removeItem_itemBelongsToOtherCart_throwsResourceNotFound() {
        Cart cart = activeCart();
        CartItem item = CartItem.builder().id(itemId).cartId(UUID.randomUUID()).build();
        when(cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findById(itemId)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> cartService.removeItem(userId, itemId))
            .isInstanceOf(ResourceNotFoundException.class);
        verify(cartItemRepository, never()).delete(any());
    }

    // ── checkout ─────────────────────────────────────────────────────────────

    @Test
    void checkout_emptyCart_throwsValidation() {
        Cart cart = activeCart();
        when(cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(cartId)).thenReturn(Collections.emptyList());

        CheckoutRequest req = new CheckoutRequest();

        assertThatThrownBy(() -> cartService.checkout(userId, req))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("empty");
    }

    @Test
    void checkout_withItems_createsOrdersAndChecksOutCart() {
        Cart cart = activeCart();
        CartItem item = CartItem.builder().id(itemId).cartId(cartId).merchId(merchId).quantity(2).build();
        when(cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(cartId)).thenReturn(List.of(item));
        when(orderService.createOrdersFromCart(any(), any(), any(), any(), any(), any(), any())).thenReturn(Collections.emptyList());
        when(cartRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CheckoutRequest req = new CheckoutRequest();

        assertThatNoException().isThrownBy(() -> cartService.checkout(userId, req));
        verify(cartRepository).save(any(Cart.class));
    }
}
