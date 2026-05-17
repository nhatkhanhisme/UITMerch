package com.uitmerch.backend.order.service;

import com.uitmerch.backend.auth.entity.User;
import com.uitmerch.backend.auth.repository.UserRepository;
import com.uitmerch.backend.cart.entity.Cart;
import com.uitmerch.backend.cart.entity.CartItem;
import com.uitmerch.backend.common.exception.ResourceNotFoundException;
import com.uitmerch.backend.common.exception.ValidationException;
import com.uitmerch.backend.common.model.CartStatus;
import com.uitmerch.backend.common.model.MerchItemStatus;
import com.uitmerch.backend.common.model.OrderStatus;
import com.uitmerch.backend.common.model.UserRole;
import com.uitmerch.backend.common.service.EmailService;
import com.uitmerch.backend.merch.entity.MerchItem;
import com.uitmerch.backend.merch.repository.MerchItemRepository;
import com.uitmerch.backend.order.dto.GuestOrderItemRequest;
import com.uitmerch.backend.order.dto.GuestOrderRequest;
import com.uitmerch.backend.order.dto.InstantOrderRequest;
import com.uitmerch.backend.order.dto.OrderResponse;
import com.uitmerch.backend.order.entity.Order;
import com.uitmerch.backend.order.entity.OrderItem;
import com.uitmerch.backend.order.repository.OrderItemRepository;
import com.uitmerch.backend.order.repository.OrderRepository;
import com.uitmerch.backend.organization.entity.Organization;
import com.uitmerch.backend.organization.service.OrganizationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private MerchItemRepository merchItemRepository;
    @Mock private OrganizationService organizationService;
    @Mock private UserRepository userRepository;
    @Mock private EmailService emailService;

    @InjectMocks private OrderService orderService;

    private final UUID userId = UUID.randomUUID();
    private final UUID orgId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();
    private final UUID merchId = UUID.randomUUID();

    private MerchItem publishedMerch(int stock) {
        return MerchItem.builder()
            .id(merchId)
            .orgId(orgId)
            .name("Test Merch")
            .price(BigDecimal.valueOf(100_000))
            .stock(stock)
            .status(MerchItemStatus.PUBLISHED)
            .build();
    }

    private Order orderWithStatus(OrderStatus status) {
        return Order.builder()
            .id(orderId)
            .userId(userId)
            .orgId(orgId)
            .totalAmount(BigDecimal.valueOf(100_000))
            .status(status)
            .build();
    }

    // ── createInstantOrder ───────────────────────────────────────────────────

    @Test
    void createInstantOrder_success_callsAtomicDeduction() {
        MerchItem merch = publishedMerch(5);
        Order savedOrder = orderWithStatus(OrderStatus.PENDING);
        OrderItem savedItem = OrderItem.builder()
            .id(UUID.randomUUID()).orderId(orderId).merchId(merchId)
            .merchName("Test Merch").unitPrice(BigDecimal.valueOf(100_000))
            .quantity(2).subtotal(BigDecimal.valueOf(200_000)).build();

        when(merchItemRepository.findById(merchId)).thenReturn(Optional.of(merch));
        when(merchItemRepository.deductStock(eq(merchId), eq(2))).thenReturn(1);
        when(orderRepository.save(any())).thenReturn(savedOrder);
        when(orderItemRepository.save(any())).thenReturn(savedItem);

        InstantOrderRequest req = new InstantOrderRequest();
        req.setMerchId(merchId);
        req.setQuantity(2);

        OrderResponse response = orderService.createInstantOrder(userId, req);

        assertThat(response.getOrgId()).isEqualTo(orgId);
        verify(merchItemRepository).deductStock(merchId, 2);
    }

    @Test
    void createInstantOrder_concurrentSellout_throwsValidation() {
        MerchItem merch = publishedMerch(1);
        when(merchItemRepository.findById(merchId)).thenReturn(Optional.of(merch));
        when(merchItemRepository.deductStock(eq(merchId), eq(1))).thenReturn(0); // another thread won

        InstantOrderRequest req = new InstantOrderRequest();
        req.setMerchId(merchId);
        req.setQuantity(1);

        assertThatThrownBy(() -> orderService.createInstantOrder(userId, req))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("sold out");
    }

    @Test
    void createInstantOrder_outOfStock_throwsValidation() {
        when(merchItemRepository.findById(merchId)).thenReturn(Optional.of(publishedMerch(1)));

        InstantOrderRequest req = new InstantOrderRequest();
        req.setMerchId(merchId);
        req.setQuantity(5); // more than stock

        assertThatThrownBy(() -> orderService.createInstantOrder(userId, req))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Insufficient stock");
    }

    @Test
    void createInstantOrder_unpublishedMerch_throwsValidation() {
        MerchItem draft = MerchItem.builder()
            .id(merchId)
            .orgId(orgId)
            .name("Draft Item")
            .price(BigDecimal.valueOf(50_000))
            .stock(10)
            .status(MerchItemStatus.DRAFT)
            .build();

        when(merchItemRepository.findById(merchId)).thenReturn(Optional.of(draft));

        InstantOrderRequest req = new InstantOrderRequest();
        req.setMerchId(merchId);
        req.setQuantity(1);

        assertThatThrownBy(() -> orderService.createInstantOrder(userId, req))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("not available");
    }

    @Test
    void createInstantOrder_merchNotFound_throwsResourceNotFound() {
        when(merchItemRepository.findById(merchId)).thenReturn(Optional.empty());

        InstantOrderRequest req = new InstantOrderRequest();
        req.setMerchId(merchId);
        req.setQuantity(1);

        assertThatThrownBy(() -> orderService.createInstantOrder(userId, req))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── updateOrderStatus (validates transitions) ────────────────────────────

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
        "PENDING,CONFIRMED",
        "PENDING,CANCELLED",
        "CONFIRMED,READY_FOR_PICKUP",
        "CONFIRMED,CANCELLED",
        "READY_FOR_PICKUP,SUCCESS"
    })
    void updateOrderStatus_validTransition_succeeds(OrderStatus from, OrderStatus to) {
        Organization org = Organization.builder().id(orgId).ownerId(userId).build();
        Order order = orderWithStatus(from);
        Order updatedOrder = orderWithStatus(to);

        when(organizationService.getOwnOrganizationEntity(userId, orgId)).thenReturn(org);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenReturn(updatedOrder);
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(Collections.emptyList());
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        OrderResponse response = orderService.updateOrderStatus(userId, orgId, orderId, to);

        assertThat(response).isNotNull();
    }

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
        "PENDING,READY_FOR_PICKUP",
        "PENDING,SUCCESS",
        "CONFIRMED,PENDING",
        "CONFIRMED,SUCCESS",
        "READY_FOR_PICKUP,PENDING",
        "READY_FOR_PICKUP,CONFIRMED",
        "READY_FOR_PICKUP,CANCELLED",
        "SUCCESS,PENDING",
        "SUCCESS,CONFIRMED",
        "SUCCESS,CANCELLED",
        "CANCELLED,PENDING",
        "CANCELLED,CONFIRMED"
    })
    void updateOrderStatus_invalidTransition_throwsValidation(OrderStatus from, OrderStatus to) {
        Organization org = Organization.builder().id(orgId).ownerId(userId).build();
        Order order = orderWithStatus(from);

        when(organizationService.getOwnOrganizationEntity(userId, orgId)).thenReturn(org);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateOrderStatus(userId, orgId, orderId, to))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Cannot transition");
    }

    @Test
    void updateOrderStatus_orderBelongsToOtherOrg_throwsResourceNotFound() {
        UUID otherOrgId = UUID.randomUUID();
        Organization org = Organization.builder().id(otherOrgId).ownerId(userId).build();
        Order order = orderWithStatus(OrderStatus.PENDING); // orgId != otherOrgId

        when(organizationService.getOwnOrganizationEntity(userId, orgId)).thenReturn(org);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateOrderStatus(userId, orgId, orderId, OrderStatus.CONFIRMED))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getCustomerOrder ─────────────────────────────────────────────────────

    @Test
    void getCustomerOrder_ownOrder_returnsResponse() {
        Order order = orderWithStatus(OrderStatus.PENDING);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(Collections.emptyList());

        OrderResponse response = orderService.getCustomerOrder(userId, orderId);

        assertThat(response.getId()).isEqualTo(orderId);
    }

    @Test
    void getCustomerOrder_otherUsersOrder_throwsResourceNotFound() {
        UUID otherUserId = UUID.randomUUID();
        Order order = orderWithStatus(OrderStatus.PENDING); // belongs to userId, not otherUserId

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.getCustomerOrder(otherUserId, orderId))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getCustomerOrder_notFound_throwsResourceNotFound() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getCustomerOrder(userId, orderId))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── updateOrderStatus: cancellation restores stock ───────────────────────

    @Test
    void updateOrderStatus_cancelledOrder_restoresStock() {
        Organization org = Organization.builder().id(orgId).ownerId(userId).build();
        Order order = orderWithStatus(OrderStatus.PENDING);
        Order cancelled = orderWithStatus(OrderStatus.CANCELLED);
        OrderItem item = OrderItem.builder().id(UUID.randomUUID()).orderId(orderId)
            .merchId(merchId).quantity(3).unitPrice(BigDecimal.valueOf(100_000))
            .subtotal(BigDecimal.valueOf(300_000)).build();

        when(organizationService.getOwnOrganizationEntity(userId, orgId)).thenReturn(org);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenReturn(cancelled);
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of(item));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        orderService.updateOrderStatus(userId, orgId, orderId, OrderStatus.CANCELLED);

        verify(merchItemRepository).restoreStock(merchId, 3);
    }

    // ── cancelCustomerOrder ──────────────────────────────────────────────────

    @Test
    void cancelCustomerOrder_pendingOrder_cancelsAndRestoresStock() {
        Order order = orderWithStatus(OrderStatus.PENDING);
        OrderItem item = OrderItem.builder().id(UUID.randomUUID()).orderId(orderId)
            .merchId(merchId).quantity(2).unitPrice(BigDecimal.valueOf(100_000))
            .subtotal(BigDecimal.valueOf(200_000)).build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenReturn(orderWithStatus(OrderStatus.CANCELLED));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of(item));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        orderService.cancelCustomerOrder(userId, orderId);

        verify(merchItemRepository).restoreStock(merchId, 2);
    }

    @Test
    void cancelCustomerOrder_confirmedOrder_throwsValidation() {
        Order order = orderWithStatus(OrderStatus.CONFIRMED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelCustomerOrder(userId, orderId))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("PENDING");
        verify(merchItemRepository, never()).restoreStock(any(), anyInt());
    }

    @Test
    void cancelCustomerOrder_wrongUser_throwsResourceNotFound() {
        UUID otherUser = UUID.randomUUID();
        Order order = orderWithStatus(OrderStatus.PENDING); // belongs to userId
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelCustomerOrder(otherUser, orderId))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── createOrdersFromCart ─────────────────────────────────────────────────

    @Test
    void createOrdersFromCart_unavailableItem_throwsBeforeDeduction() {
        Cart cart = Cart.builder().id(UUID.randomUUID()).userId(userId).status(CartStatus.ACTIVE).build();
        CartItem cartItem = CartItem.builder().id(UUID.randomUUID())
            .cartId(cart.getId()).merchId(merchId).quantity(1).build();

        MerchItem draft = MerchItem.builder().id(merchId).orgId(orgId).name("Draft")
            .price(BigDecimal.valueOf(10_000)).stock(5).status(MerchItemStatus.DRAFT).build();

        when(merchItemRepository.findAllById(any())).thenReturn(List.of(draft));

        assertThatThrownBy(() -> orderService.createOrdersFromCart(userId, cart, List.of(cartItem), null, null, null, null))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("no longer available");
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrdersFromCart_insufficientStock_throwsBeforeDeduction() {
        Cart cart = Cart.builder().id(UUID.randomUUID()).userId(userId).status(CartStatus.ACTIVE).build();
        CartItem cartItem = CartItem.builder().id(UUID.randomUUID())
            .cartId(cart.getId()).merchId(merchId).quantity(10).build();

        MerchItem merch = publishedMerch(2); // only 2 in stock
        when(merchItemRepository.findAllById(any())).thenReturn(List.of(merch));

        assertThatThrownBy(() -> orderService.createOrdersFromCart(userId, cart, List.of(cartItem), null, null, null, null))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Insufficient stock");
        verify(orderRepository, never()).save(any());
    }

    // ── createGuestOrder ─────────────────────────────────────────────────────

    @Test
    void createGuestOrder_insufficientStock_throwsBeforeCreatingOrder() {
        GuestOrderItemRequest item = new GuestOrderItemRequest();
        item.setMerchId(merchId);
        item.setQuantity(99);

        GuestOrderRequest req = new GuestOrderRequest();
        req.setItems(List.of(item));
        req.setGuestName("Guest");
        req.setGuestPhone("0901");
        req.setGuestAddress("Addr");

        MerchItem merch = publishedMerch(1);
        when(merchItemRepository.findAllById(any())).thenReturn(List.of(merch));

        assertThatThrownBy(() -> orderService.createGuestOrder(req))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Insufficient stock");
        verify(orderRepository, never()).save(any());
    }

    // ── getGuestOrderByEmail ─────────────────────────────────────────────────

    @Test
    void getGuestOrderByEmail_matchingEmail_returnsOrder() {
        Order guestOrder = Order.builder().id(orderId).orgId(orgId)
            .guestEmail("guest@test.com").totalAmount(BigDecimal.ZERO)
            .status(OrderStatus.PENDING).build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(guestOrder));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(Collections.emptyList());

        OrderResponse response = orderService.getGuestOrderByEmail(orderId, "guest@test.com");

        assertThat(response.getId()).isEqualTo(orderId);
    }

    @Test
    void getGuestOrderByEmail_wrongEmail_throwsResourceNotFound() {
        Order guestOrder = Order.builder().id(orderId).orgId(orgId)
            .guestEmail("real@test.com").totalAmount(BigDecimal.ZERO)
            .status(OrderStatus.PENDING).build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(guestOrder));

        assertThatThrownBy(() -> orderService.getGuestOrderByEmail(orderId, "wrong@test.com"))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getGuestOrderByEmail_authenticatedOrder_throwsResourceNotFound() {
        Order authOrder = orderWithStatus(OrderStatus.PENDING); // has userId set, no guestEmail

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(authOrder));

        assertThatThrownBy(() -> orderService.getGuestOrderByEmail(orderId, "any@test.com"))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
