package com.uitmerch.backend.orders.service;

import com.uitmerch.backend.auth.entity.User;
import com.uitmerch.backend.auth.repository.UserRepository;
import com.uitmerch.backend.carts.entity.Cart;
import com.uitmerch.backend.carts.entity.CartItem;
import com.uitmerch.backend.carts.repository.CartItemRepository;
import com.uitmerch.backend.carts.repository.CartRepository;
import com.uitmerch.backend.catalog.entity.MerchItem;
import com.uitmerch.backend.common.exception.ValidationException;
import com.uitmerch.backend.common.model.CartStatus;
import com.uitmerch.backend.common.model.OrderStatus;
import com.uitmerch.backend.common.model.PaymentMethod;
import com.uitmerch.backend.common.model.UserRole;
import com.uitmerch.backend.orders.dto.CheckoutRequest;
import com.uitmerch.backend.orders.dto.CheckoutResponse;
import com.uitmerch.backend.orders.entity.Order;
import com.uitmerch.backend.orders.entity.OrderItem;
import com.uitmerch.backend.orders.repository.OrderItemRepository;
import com.uitmerch.backend.orders.repository.OrderRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(
            orderRepository,
            orderItemRepository,
            cartRepository,
            cartItemRepository,
            userRepository,
            applicationEventPublisher
        );

        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("customer@example.com", null)
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void checkout_createsCodOrderWhenStockIsAvailable() {
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID cartId = UUID.randomUUID();
        UUID merchItemId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID orderItemId = UUID.randomUUID();

        User currentUser = buildUser(userId);
        OrganizationFixture organizationFixture = buildOrganization(organizationId, currentUser);
        Cart cart = buildCart(cartId, currentUser, organizationFixture.organization(), CartStatus.ACTIVE);
        MerchItem merchItem = buildMerchItem(merchItemId, organizationFixture.organization(), new BigDecimal("150000.00"), 5, false);
        CartItem cartItem = buildCartItem(UUID.randomUUID(), cart, merchItem, 1);

        CheckoutRequest request = CheckoutRequest.builder()
            .idempotencyKey("idem-001")
            .build();

        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(currentUser));
        when(orderRepository.findByIdempotencyKey("idem-001")).thenReturn(Optional.empty());
        when(cartRepository.findByCustomerUserAndStatusOrderByCreatedAtAsc(currentUser, CartStatus.ACTIVE)).thenReturn(List.of(cart));
        when(cartItemRepository.findByCartId(cartId)).thenReturn(List.of(cartItem));
        when(orderRepository.existsByOrderCode(anyString())).thenReturn(false);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(orderId);
            order.setCreatedAt(Instant.parse("2026-05-06T10:30:00Z"));
            order.setUpdatedAt(Instant.parse("2026-05-06T10:30:00Z"));
            return order;
        });
        when(orderItemRepository.saveAll(any())).thenAnswer(invocation -> {
            List<OrderItem> items = invocation.getArgument(0);
            items.get(0).setId(orderItemId);
            return items;
        });
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CheckoutResponse response = orderService.checkout(request);

        assertNotNull(response);
        assertEquals(orderId, response.getId());
        assertEquals("idem-001", request.getIdempotencyKey());
        assertEquals(PaymentMethod.CASH_ON_DELIVERY, response.getPaymentMethod());
        assertEquals(OrderStatus.PENDING, response.getStatus());
        assertEquals(new BigDecimal("150000.00"), response.getTotalAmount());
        assertEquals(1, response.getItems().size());
        assertEquals(orderItemId, response.getItems().get(0).getId());
        assertEquals(4, merchItem.getStockQuantity());
        assertEquals(CartStatus.CHECKED_OUT, cart.getStatus());

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertEquals(currentUser, orderCaptor.getValue().getCustomerUser());
        assertEquals(organizationFixture.organization(), orderCaptor.getValue().getOrganization());
        verify(orderItemRepository).saveAll(any());
        verify(cartRepository).save(cart);
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void checkout_withDuplicateIdempotencyKey_returnsExistingOrderWithoutCreatingAnother() {
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID merchItemId = UUID.randomUUID();
        UUID orderItemId = UUID.randomUUID();

        User currentUser = buildUser(userId);
        OrganizationFixture organizationFixture = buildOrganization(organizationId, currentUser);
        Order existingOrder = buildOrder(orderId, currentUser, organizationFixture.organization(), "idem-duplicate");
        OrderItem existingOrderItem = buildOrderItem(orderItemId, existingOrder, buildMerchItem(merchItemId, organizationFixture.organization(), new BigDecimal("120000.00"), 3, false), 2);

        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(currentUser));
        when(orderRepository.findByIdempotencyKey("idem-duplicate")).thenReturn(Optional.of(existingOrder));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of(existingOrderItem));

        CheckoutResponse response = orderService.checkout(CheckoutRequest.builder().idempotencyKey("idem-duplicate").build());

        assertNotNull(response);
        assertEquals(orderId, response.getId());
        assertEquals("ORD-EXISTING", response.getOrderCode());
        assertEquals(OrderStatus.PENDING, response.getStatus());
        assertEquals(1, response.getItems().size());
        assertEquals(orderItemId, response.getItems().get(0).getId());
        assertEquals(new BigDecimal("240000.00"), response.getTotalAmount());
        assertEquals(PaymentMethod.CASH_ON_DELIVERY, response.getPaymentMethod());

        verify(orderRepository, never()).save(any(Order.class));
        verify(cartRepository, never()).save(any(Cart.class));
        verify(orderItemRepository, never()).saveAll(any());
        verifyNoMoreInteractions(cartRepository, cartItemRepository, applicationEventPublisher);
    }

    @Test
    void checkout_throwsWhenStockIsZero() {
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID cartId = UUID.randomUUID();
        UUID merchItemId = UUID.randomUUID();

        User currentUser = buildUser(userId);
        OrganizationFixture organizationFixture = buildOrganization(organizationId, currentUser);
        Cart cart = buildCart(cartId, currentUser, organizationFixture.organization(), CartStatus.ACTIVE);
        MerchItem merchItem = buildMerchItem(merchItemId, organizationFixture.organization(), new BigDecimal("50000.00"), 0, false);
        CartItem cartItem = buildCartItem(UUID.randomUUID(), cart, merchItem, 1);

        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(currentUser));
        when(orderRepository.findByIdempotencyKey("idem-zero-stock")).thenReturn(Optional.empty());
        when(cartRepository.findByCustomerUserAndStatusOrderByCreatedAtAsc(currentUser, CartStatus.ACTIVE)).thenReturn(List.of(cart));
        when(cartItemRepository.findByCartId(cartId)).thenReturn(List.of(cartItem));

        ValidationException exception = assertThrows(
            ValidationException.class,
            () -> orderService.checkout(CheckoutRequest.builder().idempotencyKey("idem-zero-stock").build())
        );

        assertEquals("Insufficient stock for merch item: UIT Hoodie", exception.getMessage());
        verify(orderRepository, never()).save(any(Order.class));
        verify(orderItemRepository, never()).saveAll(any());
        verify(cartRepository, never()).save(any(Cart.class));
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    private User buildUser(UUID id) {
        return User.builder()
            .id(id)
            .email("customer@example.com")
            .passwordHash("hashed-password")
            .fullName("Test Customer")
            .role(UserRole.CUSTOMER)
            .createdAt(Instant.parse("2026-05-06T00:00:00Z"))
            .build();
    }

    private OrganizationFixture buildOrganization(UUID id, User owner) {
        com.uitmerch.backend.organizations.entity.Organization organization = com.uitmerch.backend.organizations.entity.Organization.builder()
            .id(id)
            .ownerUser(owner)
            .name("UIT Merch Org")
            .description("Organizer org")
            .logoUrl(null)
            .status(com.uitmerch.backend.common.model.OrganizationStatus.ACTIVE)
            .createdAt(Instant.parse("2026-05-06T00:00:00Z"))
            .updatedAt(Instant.parse("2026-05-06T00:00:00Z"))
            .build();
        return new OrganizationFixture(organization);
    }

    private Cart buildCart(UUID id, User customerUser, com.uitmerch.backend.organizations.entity.Organization organization, CartStatus status) {
        return Cart.builder()
            .id(id)
            .customerUser(customerUser)
            .organization(organization)
            .status(status)
            .createdAt(Instant.parse("2026-05-06T10:00:00Z"))
            .updatedAt(Instant.parse("2026-05-06T10:00:00Z"))
            .build();
    }

    private MerchItem buildMerchItem(UUID id, com.uitmerch.backend.organizations.entity.Organization organization, BigDecimal price, Integer stockQuantity, Boolean preorder) {
        return MerchItem.builder()
            .id(id)
            .organization(organization)
            .name("UIT Hoodie")
            .meaningText("Official hoodie")
            .price(price)
            .stockQuantity(stockQuantity)
            .isPreorder(preorder)
            .isActive(true)
            .imageUrl(null)
            .createdAt(Instant.parse("2026-05-06T10:00:00Z"))
            .updatedAt(Instant.parse("2026-05-06T10:00:00Z"))
            .build();
    }

    private CartItem buildCartItem(UUID id, Cart cart, MerchItem merchItem, Integer quantity) {
        return CartItem.builder()
            .id(id)
            .cart(cart)
            .merchItem(merchItem)
            .quantity(quantity)
            .createdAt(Instant.parse("2026-05-06T10:05:00Z"))
            .updatedAt(Instant.parse("2026-05-06T10:05:00Z"))
            .build();
    }

    private Order buildOrder(UUID id, User customerUser, com.uitmerch.backend.organizations.entity.Organization organization, String idempotencyKey) {
        return Order.builder()
            .id(id)
            .orderCode("ORD-EXISTING")
            .customerUser(customerUser)
            .organization(organization)
            .idempotencyKey(idempotencyKey)
            .status(OrderStatus.PENDING)
            .totalAmount(new BigDecimal("240000.00"))
            .paymentMethod(PaymentMethod.CASH_ON_DELIVERY)
            .placedAt(Instant.parse("2026-05-06T10:10:00Z"))
            .createdAt(Instant.parse("2026-05-06T10:10:00Z"))
            .updatedAt(Instant.parse("2026-05-06T10:10:00Z"))
            .build();
    }

    private OrderItem buildOrderItem(UUID id, Order order, MerchItem merchItem, Integer quantity) {
        return OrderItem.builder()
            .id(id)
            .order(order)
            .merchItem(merchItem)
            .unitPriceSnapshot(merchItem.getPrice())
            .quantity(quantity)
            .createdAt(Instant.parse("2026-05-06T10:10:30Z"))
            .build();
    }

    private record OrganizationFixture(com.uitmerch.backend.organizations.entity.Organization organization) {
    }
}