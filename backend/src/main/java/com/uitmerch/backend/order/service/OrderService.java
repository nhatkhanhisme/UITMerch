package com.uitmerch.backend.order.service;

import com.uitmerch.backend.auth.repository.UserRepository;
import com.uitmerch.backend.cart.entity.Cart;
import com.uitmerch.backend.cart.entity.CartItem;
import com.uitmerch.backend.common.exception.ResourceNotFoundException;
import com.uitmerch.backend.common.exception.ValidationException;
import com.uitmerch.backend.common.model.MerchItemStatus;
import com.uitmerch.backend.common.model.OrderStatus;
import com.uitmerch.backend.common.service.EmailService;
import com.uitmerch.backend.merch.entity.MerchItem;
import com.uitmerch.backend.merch.repository.MerchItemRepository;
import com.uitmerch.backend.order.dto.*;
import com.uitmerch.backend.order.entity.Order;
import com.uitmerch.backend.order.entity.OrderItem;
import com.uitmerch.backend.order.repository.OrderItemRepository;
import com.uitmerch.backend.order.repository.OrderRepository;
import com.uitmerch.backend.organization.entity.Organization;
import com.uitmerch.backend.organization.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final MerchItemRepository merchItemRepository;
    private final OrganizationService organizationService;
    private final UserRepository userRepository;
    private final EmailService emailService;

    // ------------------------------------------------------------------ //
    //  CART CHECKOUT
    // ------------------------------------------------------------------ //

    @Transactional
    public List<OrderResponse> createOrdersFromCart(UUID userId, Cart cart, List<CartItem> cartItems, String note) {
        // Batch-fetch all merch items
        List<UUID> merchIds = cartItems.stream().map(CartItem::getMerchId).toList();
        Map<UUID, MerchItem> merchMap = merchItemRepository.findAllById(merchIds)
            .stream().collect(Collectors.toMap(MerchItem::getId, m -> m));

        // Validate ALL items before any mutation
        for (CartItem cartItem : cartItems) {
            MerchItem merch = merchMap.get(cartItem.getMerchId());
            if (merch == null || merch.getStatus() != MerchItemStatus.PUBLISHED) {
                String name = merch != null ? merch.getName() : cartItem.getMerchId().toString();
                throw new ValidationException("Item is no longer available: " + name);
            }
            if (merch.getStock() < cartItem.getQuantity()) {
                throw new ValidationException(
                    "Insufficient stock for \"" + merch.getName() + "\". Available: " + merch.getStock()
                );
            }
        }

        // Group cart items by orgId
        Map<UUID, List<CartItem>> byOrg = cartItems.stream()
            .collect(Collectors.groupingBy(ci -> merchMap.get(ci.getMerchId()).getOrgId()));

        List<OrderResponse> results = new ArrayList<>();

        for (Map.Entry<UUID, List<CartItem>> entry : byOrg.entrySet()) {
            UUID orgId = entry.getKey();
            List<CartItem> orgItems = entry.getValue();

            // Build order items and compute total
            List<OrderItem> orderItems = new ArrayList<>();
            BigDecimal total = BigDecimal.ZERO;

            for (CartItem cartItem : orgItems) {
                MerchItem merch = merchMap.get(cartItem.getMerchId());
                BigDecimal subtotal = merch.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
                total = total.add(subtotal);

                orderItems.add(OrderItem.builder()
                    .merchId(merch.getId())
                    .merchName(merch.getName())
                    .unitPrice(merch.getPrice())
                    .quantity(cartItem.getQuantity())
                    .subtotal(subtotal)
                    .build());

                // Atomically deduct stock; 0 rows updated means a concurrent order beat us
                if (merchItemRepository.deductStock(merch.getId(), cartItem.getQuantity()) == 0) {
                    throw new ValidationException(
                        "\"" + merch.getName() + "\" just sold out — please update your cart."
                    );
                }
            }

            Order order = Order.builder()
                .userId(userId)
                .orgId(orgId)
                .totalAmount(total)
                .note(note)
                .build();
            order = orderRepository.save(order);

            final UUID orderId = order.getId();
            orderItems.forEach(item -> item.setOrderId(orderId));
            List<OrderItem> savedItems = orderItemRepository.saveAll(orderItems);

            results.add(OrderResponse.from(order, savedItems));
        }

        return results;
    }

    // ------------------------------------------------------------------ //
    //  INSTANT ORDER (authenticated customer)
    // ------------------------------------------------------------------ //

    @Transactional
    public OrderResponse createInstantOrder(UUID userId, InstantOrderRequest request) {
        MerchItem merch = merchItemRepository.findById(request.getMerchId())
            .orElseThrow(() -> new ResourceNotFoundException("Merch item", request.getMerchId().toString()));

        if (merch.getStatus() != MerchItemStatus.PUBLISHED) {
            throw new ValidationException("This item is not available for purchase.");
        }
        if (merch.getStock() < request.getQuantity()) {
            throw new ValidationException(
                "Insufficient stock for \"" + merch.getName() + "\". Available: " + merch.getStock()
            );
        }

        BigDecimal subtotal = merch.getPrice().multiply(BigDecimal.valueOf(request.getQuantity()));

        // Deduct stock atomically before creating the order
        if (merchItemRepository.deductStock(merch.getId(), request.getQuantity()) == 0) {
            throw new ValidationException(
                "\"" + merch.getName() + "\" just sold out — please try again."
            );
        }

        Order order = Order.builder()
            .userId(userId)
            .orgId(merch.getOrgId())
            .totalAmount(subtotal)
            .note(request.getNote())
            .build();
        order = orderRepository.save(order);

        OrderItem orderItem = OrderItem.builder()
            .orderId(order.getId())
            .merchId(merch.getId())
            .merchName(merch.getName())
            .unitPrice(merch.getPrice())
            .quantity(request.getQuantity())
            .subtotal(subtotal)
            .build();
        orderItem = orderItemRepository.save(orderItem);

        return OrderResponse.from(order, List.of(orderItem));
    }

    // ------------------------------------------------------------------ //
    //  GUEST ORDER
    // ------------------------------------------------------------------ //

    @Transactional
    public List<OrderResponse> createGuestOrder(GuestOrderRequest request) {
        List<GuestOrderItemRequest> items = request.getItems();
        List<UUID> merchIds = items.stream().map(GuestOrderItemRequest::getMerchId).toList();
        Map<UUID, MerchItem> merchMap = merchItemRepository.findAllById(merchIds)
            .stream().collect(Collectors.toMap(MerchItem::getId, m -> m));

        // Validate ALL items first
        for (GuestOrderItemRequest item : items) {
            MerchItem merch = merchMap.get(item.getMerchId());
            if (merch == null || merch.getStatus() != MerchItemStatus.PUBLISHED) {
                String name = merch != null ? merch.getName() : item.getMerchId().toString();
                throw new ValidationException("Item is no longer available: " + name);
            }
            if (merch.getStock() < item.getQuantity()) {
                throw new ValidationException(
                    "Insufficient stock for \"" + merch.getName() + "\". Available: " + merch.getStock()
                );
            }
        }

        // Group by orgId
        Map<UUID, List<GuestOrderItemRequest>> byOrg = items.stream()
            .collect(Collectors.groupingBy(i -> merchMap.get(i.getMerchId()).getOrgId()));

        List<OrderResponse> results = new ArrayList<>();

        for (Map.Entry<UUID, List<GuestOrderItemRequest>> entry : byOrg.entrySet()) {
            UUID orgId = entry.getKey();
            List<GuestOrderItemRequest> orgItems = entry.getValue();

            List<OrderItem> orderItems = new ArrayList<>();
            BigDecimal total = BigDecimal.ZERO;

            for (GuestOrderItemRequest item : orgItems) {
                MerchItem merch = merchMap.get(item.getMerchId());
                BigDecimal subtotal = merch.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                total = total.add(subtotal);

                orderItems.add(OrderItem.builder()
                    .merchId(merch.getId())
                    .merchName(merch.getName())
                    .unitPrice(merch.getPrice())
                    .quantity(item.getQuantity())
                    .subtotal(subtotal)
                    .build());

                if (merchItemRepository.deductStock(merch.getId(), item.getQuantity()) == 0) {
                    throw new ValidationException(
                        "\"" + merch.getName() + "\" just sold out — please try again."
                    );
                }
            }

            Order order = Order.builder()
                .userId(null)
                .orgId(orgId)
                .guestName(request.getGuestName())
                .guestEmail(request.getGuestEmail())
                .guestPhone(request.getGuestPhone())
                .guestAddress(request.getGuestAddress())
                .totalAmount(total)
                .note(request.getNote())
                .build();
            order = orderRepository.save(order);

            final UUID orderId = order.getId();
            orderItems.forEach(item -> item.setOrderId(orderId));
            List<OrderItem> savedItems = orderItemRepository.saveAll(orderItems);

            results.add(OrderResponse.from(order, savedItems));
        }

        return results;
    }

    // ------------------------------------------------------------------ //
    //  CUSTOMER READ
    // ------------------------------------------------------------------ //

    @Transactional(readOnly = true)
    public Page<OrderResponse> getCustomerOrders(UUID userId, OrderStatus statusFilter, Pageable pageable) {
        Page<Order> orders = statusFilter != null
            ? orderRepository.findByUserIdAndStatus(userId, statusFilter, pageable)
            : orderRepository.findByUserId(userId, pageable);

        return orders.map(order -> {
            List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
            return OrderResponse.from(order, items);
        });
    }

    @Transactional(readOnly = true)
    public OrderResponse getCustomerOrder(UUID userId, UUID orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order", orderId.toString()));

        if (!userId.equals(order.getUserId())) {
            throw new ResourceNotFoundException("Order", orderId.toString());
        }

        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        return OrderResponse.from(order, items);
    }

    // ------------------------------------------------------------------ //
    //  ORGANIZER READ + STATUS UPDATE
    // ------------------------------------------------------------------ //

    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrgOrders(UUID ownerId, UUID orgId, OrderStatus statusFilter, Pageable pageable) {
        Organization org = organizationService.getOwnOrganizationEntity(ownerId, orgId);
        UUID resolvedOrgId = org.getId();

        Page<Order> orders = statusFilter != null
            ? orderRepository.findByOrgIdAndStatus(resolvedOrgId, statusFilter, pageable)
            : orderRepository.findByOrgId(resolvedOrgId, pageable);

        return orders.map(order -> {
            List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
            return OrderResponse.from(order, items);
        });
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrgOrder(UUID ownerId, UUID orgId, UUID orderId) {
        Organization org = organizationService.getOwnOrganizationEntity(ownerId, orgId);

        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order", orderId.toString()));

        if (!org.getId().equals(order.getOrgId())) {
            throw new ResourceNotFoundException("Order", orderId.toString());
        }

        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        return OrderResponse.from(order, items);
    }

    @Transactional
    public OrderResponse updateOrderStatus(UUID ownerId, UUID orgId, UUID orderId, OrderStatus newStatus) {
        Organization org = organizationService.getOwnOrganizationEntity(ownerId, orgId);

        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order", orderId.toString()));

        if (!org.getId().equals(order.getOrgId())) {
            throw new ResourceNotFoundException("Order", orderId.toString());
        }

        validateStatusTransition(order.getStatus(), newStatus);

        order.setStatus(newStatus);
        order = orderRepository.save(order);

        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);

        if (newStatus == OrderStatus.CANCELLED) {
            restoreStockForItems(items);
        }

        notifyCustomer(order, newStatus);

        return OrderResponse.from(order, items);
    }

    // ------------------------------------------------------------------ //
    //  CUSTOMER CANCELLATION
    // ------------------------------------------------------------------ //

    @Transactional
    public OrderResponse cancelCustomerOrder(UUID userId, UUID orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order", orderId.toString()));

        if (!userId.equals(order.getUserId())) {
            throw new ResourceNotFoundException("Order", orderId.toString());
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new ValidationException(
                "Only PENDING orders can be cancelled. Current status: " + order.getStatus()
            );
        }

        order.setStatus(OrderStatus.CANCELLED);
        order = orderRepository.save(order);

        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        restoreStockForItems(items);
        notifyCustomer(order, OrderStatus.CANCELLED);

        return OrderResponse.from(order, items);
    }

    // ------------------------------------------------------------------ //
    //  ADMIN
    // ------------------------------------------------------------------ //

    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(OrderStatus statusFilter, Pageable pageable) {
        Page<Order> orders = statusFilter != null
            ? orderRepository.findByStatus(statusFilter, pageable)
            : orderRepository.findAll(pageable);

        return orders.map(order -> {
            List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
            return OrderResponse.from(order, items);
        });
    }

    // ------------------------------------------------------------------ //
    //  HELPERS
    // ------------------------------------------------------------------ //

    /**
     * Validates order status transitions per BR05:
     * PENDING → CONFIRMED, PENDING → CANCELLED
     * CONFIRMED → READY_FOR_PICKUP, CONFIRMED → CANCELLED
     * READY_FOR_PICKUP → SUCCESS
     */
    private void notifyCustomer(Order order, OrderStatus newStatus) {
        try {
            String email = null;
            if (order.getUserId() != null) {
                email = userRepository.findById(order.getUserId())
                    .map(u -> u.getEmail()).orElse(null);
            } else if (order.getGuestEmail() != null && !order.getGuestEmail().isBlank()) {
                email = order.getGuestEmail();
            }
            if (email != null) {
                emailService.sendOrderStatusUpdate(email, order.getId().toString(), newStatus.name());
            }
        } catch (Exception e) {
            log.warn("Failed to send order status email for order {}: {}", order.getId(), e.getMessage());
        }
    }

    private void restoreStockForItems(List<OrderItem> items) {
        items.forEach(item -> merchItemRepository.restoreStock(item.getMerchId(), item.getQuantity()));
    }

    private void validateStatusTransition(OrderStatus current, OrderStatus next) {
        boolean valid = switch (current) {
            case PENDING -> next == OrderStatus.CONFIRMED || next == OrderStatus.CANCELLED;
            case CONFIRMED -> next == OrderStatus.READY_FOR_PICKUP || next == OrderStatus.CANCELLED;
            case READY_FOR_PICKUP -> next == OrderStatus.SUCCESS;
            case SUCCESS, CANCELLED -> false;
        };

        if (!valid) {
            throw new ValidationException(
                "Cannot transition order from " + current + " to " + next + "."
            );
        }
    }
}
