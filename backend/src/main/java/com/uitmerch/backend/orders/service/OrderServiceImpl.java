package com.uitmerch.backend.orders.service;

import com.uitmerch.backend.auth.entity.User;
import com.uitmerch.backend.auth.repository.UserRepository;
import com.uitmerch.backend.catalog.entity.MerchItem;
import com.uitmerch.backend.carts.entity.Cart;
import com.uitmerch.backend.carts.entity.CartItem;
import com.uitmerch.backend.carts.repository.CartItemRepository;
import com.uitmerch.backend.carts.repository.CartRepository;
import com.uitmerch.backend.common.exception.ForbiddenException;
import com.uitmerch.backend.common.exception.ResourceNotFoundException;
import com.uitmerch.backend.common.exception.ValidationException;
import com.uitmerch.backend.common.model.CartStatus;
import com.uitmerch.backend.common.model.OrderStatus;
import com.uitmerch.backend.common.model.PaymentMethod;
import com.uitmerch.backend.orders.dto.CheckoutRequest;
import com.uitmerch.backend.orders.dto.CheckoutResponse;
import com.uitmerch.backend.orders.dto.OrderItemResponse;
import com.uitmerch.backend.orders.dto.UpdateOrderStatusRequest;
import com.uitmerch.backend.orders.dto.UpdateOrderStatusResponse;
import com.uitmerch.backend.orders.entity.Order;
import com.uitmerch.backend.orders.entity.OrderItem;
import com.uitmerch.backend.orders.event.OrderSucceededEvent;
import com.uitmerch.backend.orders.repository.OrderItemRepository;
import com.uitmerch.backend.orders.repository.OrderRepository;
import com.uitmerch.backend.orders.util.OrderStatusTransitionValidator;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Checkout orchestration for customer orders.
 * Enforces BR03 (single-org cart) and BR04 (idempotency key).
 * Stock deduction and order creation occur within one @Transactional boundary.
 */
@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    public OrderServiceImpl(
        OrderRepository orderRepository,
        OrderItemRepository orderItemRepository,
        CartRepository cartRepository,
        CartItemRepository cartItemRepository,
        UserRepository userRepository,
        ApplicationEventPublisher applicationEventPublisher
    ) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.userRepository = userRepository;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    @Transactional
    public CheckoutResponse checkout(CheckoutRequest request) {
        User currentUser = getCurrentUser();

        return orderRepository.findByIdempotencyKey(request.getIdempotencyKey())
            .map(this::toResponseWithItems)
            .orElseGet(() -> createOrder(currentUser, request.getIdempotencyKey()));
    }

    @Override
    @Transactional
    public UpdateOrderStatusResponse updateOrderStatus(UUID orderId, UpdateOrderStatusRequest request) {
        User currentUser = getCurrentUser();

        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getOrganization() == null || order.getOrganization().getOwnerUser() == null
            || !currentUser.getId().equals(order.getOrganization().getOwnerUser().getId())) {
            throw new ForbiddenException("You can only update orders for your own organization");
        }

        OrderStatusTransitionValidator.validateTransition(order.getStatus(), request.getNewStatus());

        Instant now = Instant.now();
        order.setStatus(request.getNewStatus());

        if (request.getNewStatus() == OrderStatus.CONFIRMED) {
            order.setConfirmedAt(now);
        } else if (request.getNewStatus() == OrderStatus.READY_FOR_PICKUP) {
            order.setReadyAt(now);
        } else if (request.getNewStatus() == OrderStatus.SUCCESS) {
            order.setCompletedAt(now);
        } else if (request.getNewStatus() == OrderStatus.CANCELLED) {
            order.setCancelledAt(now);
        }

        Order savedOrder = orderRepository.saveAndFlush(order);

        if (request.getNewStatus() == OrderStatus.SUCCESS) {
            applicationEventPublisher.publishEvent(new OrderSucceededEvent(savedOrder.getId()));
        }

        return UpdateOrderStatusResponse.builder()
            .id(savedOrder.getId())
            .orderCode(savedOrder.getOrderCode())
            .organizationId(savedOrder.getOrganization() != null ? savedOrder.getOrganization().getId() : null)
            .status(savedOrder.getStatus())
            .updatedAt(savedOrder.getUpdatedAt())
            .build();
    }

    private CheckoutResponse createOrder(User currentUser, String idempotencyKey) {
        List<Cart> activeCarts = cartRepository.findByCustomerUserAndStatusOrderByCreatedAtAsc(currentUser, CartStatus.ACTIVE);
        if (activeCarts.isEmpty()) {
            throw new ResourceNotFoundException("Active cart not found for checkout");
        }

        if (activeCarts.size() > 1) {
            throw new ValidationException("Multiple active carts found. Checkout requires a single active cart for the customer");
        }

        Cart cart = activeCarts.get(0);
        List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId());
        if (cartItems.isEmpty()) {
            throw new ValidationException("Cannot checkout an empty cart");
        }

        UUID organizationId = cart.getOrganization().getId();
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItem cartItem : cartItems) {
            MerchItem merchItem = cartItem.getMerchItem();

            if (merchItem == null) {
                throw new ValidationException("Cart item is missing merch reference");
            }

            if (merchItem.getOrganization() == null || !organizationId.equals(merchItem.getOrganization().getId())) {
                throw new ValidationException("BR03 violation: cart contains items from multiple organizations");
            }

            if (!Boolean.TRUE.equals(merchItem.getIsPreorder())) {
                Integer stockQuantity = merchItem.getStockQuantity();
                if (stockQuantity == null || stockQuantity < cartItem.getQuantity()) {
                    throw new ValidationException("Insufficient stock for merch item: " + merchItem.getName());
                }

                merchItem.setStockQuantity(stockQuantity - cartItem.getQuantity());
            }

            BigDecimal unitPrice = merchItem.getPrice();
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            totalAmount = totalAmount.add(subtotal);

            OrderItem orderItem = OrderItem.builder()
                .merchItem(merchItem)
                .unitPriceSnapshot(unitPrice)
                .quantity(cartItem.getQuantity())
                .build();
            orderItems.add(orderItem);
        }

        String orderCode = generateOrderCode();

        Order order = Order.builder()
            .orderCode(orderCode)
            .customerUser(currentUser)
            .organization(cart.getOrganization())
            .idempotencyKey(idempotencyKey)
            .status(OrderStatus.PENDING)
            .totalAmount(totalAmount)
            .paymentMethod(PaymentMethod.CASH_ON_DELIVERY)
            .placedAt(Instant.now())
            .build();

        Order savedOrder = orderRepository.save(order);

        for (OrderItem orderItem : orderItems) {
            orderItem.setOrder(savedOrder);
        }
        orderItemRepository.saveAll(orderItems);

        cart.setStatus(CartStatus.CHECKED_OUT);
        cartRepository.save(cart);

        return toResponse(savedOrder, orderItems);
    }

    private CheckoutResponse toResponseWithItems(Order order) {
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(order.getId());
        return toResponse(order, orderItems);
    }

    private CheckoutResponse toResponse(Order order, List<OrderItem> orderItems) {
        List<OrderItemResponse> itemResponses = new ArrayList<>();
        for (OrderItem orderItem : orderItems) {
            MerchItem merchItem = orderItem.getMerchItem();
            BigDecimal subtotal = orderItem.getUnitPriceSnapshot().multiply(BigDecimal.valueOf(orderItem.getQuantity()));
            itemResponses.add(OrderItemResponse.builder()
                .id(orderItem.getId())
                .merchItemId(merchItem != null ? merchItem.getId() : null)
                .name(merchItem != null ? merchItem.getName() : null)
                .quantity(orderItem.getQuantity())
                .unitPrice(orderItem.getUnitPriceSnapshot())
                .subtotal(subtotal)
                .build());
        }

        return CheckoutResponse.builder()
            .id(order.getId())
            .orderCode(order.getOrderCode())
            .organizationId(order.getOrganization() != null ? order.getOrganization().getId() : null)
            .totalAmount(order.getTotalAmount())
            .paymentMethod(order.getPaymentMethod())
            .status(order.getStatus())
            .items(itemResponses)
            .placedAt(order.getPlacedAt())
            .createdAt(order.getCreatedAt())
            .updatedAt(order.getUpdatedAt())
            .build();
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getName() == null) {
            throw new ValidationException("Authenticated user context is missing");
        }

        return userRepository.findByEmail(authentication.getName())
            .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }

    private String generateOrderCode() {
        String orderCode;
        do {
            orderCode = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (orderRepository.existsByOrderCode(orderCode));
        return orderCode;
    }
}