package com.uitmerch.backend.order.service;

import com.uitmerch.backend.auth.repository.UserRepository;
import com.uitmerch.backend.cart.entity.Cart;
import com.uitmerch.backend.cart.entity.CartItem;
import com.uitmerch.backend.common.exception.ResourceNotFoundException;
import com.uitmerch.backend.common.exception.ValidationException;
import com.uitmerch.backend.common.model.MerchItemStatus;
import com.uitmerch.backend.common.model.NotificationType;
import com.uitmerch.backend.common.model.OrderStatus;
import com.uitmerch.backend.common.service.EmailService;
import com.uitmerch.backend.merch.entity.MerchItem;
import com.uitmerch.backend.merch.repository.MerchItemRepository;
import com.uitmerch.backend.notification.service.NotificationService;
import com.uitmerch.backend.order.dto.*;
import com.uitmerch.backend.order.entity.Order;
import com.uitmerch.backend.order.entity.OrderItem;
import com.uitmerch.backend.order.entity.PickupSchedule;
import com.uitmerch.backend.order.repository.OrderItemRepository;
import com.uitmerch.backend.order.repository.OrderRepository;
import com.uitmerch.backend.order.repository.PickupScheduleRepository;
import com.uitmerch.backend.organization.entity.Organization;
import com.uitmerch.backend.organization.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private final NotificationService notificationService;
    private final PickupScheduleRepository pickupScheduleRepository;

    // ------------------------------------------------------------------ //
    //  CART CHECKOUT
    // ------------------------------------------------------------------ //

    @Transactional
    public List<OrderResponse> createOrdersFromCart(UUID userId, Cart cart, List<CartItem> cartItems, String note,
            String shippingName, String shippingPhone, String shippingAddress) {
        List<UUID> merchIds = cartItems.stream().map(CartItem::getMerchId).toList();
        Map<UUID, MerchItem> merchMap = merchItemRepository.findAllById(merchIds)
            .stream().collect(Collectors.toMap(MerchItem::getId, m -> m));

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

        Map<UUID, List<CartItem>> byOrg = cartItems.stream()
            .collect(Collectors.groupingBy(ci -> merchMap.get(ci.getMerchId()).getOrgId()));

        List<OrderResponse> results = new ArrayList<>();

        for (Map.Entry<UUID, List<CartItem>> entry : byOrg.entrySet()) {
            UUID orgId = entry.getKey();
            List<CartItem> orgItems = entry.getValue();

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
                .guestName(shippingName)
                .guestPhone(shippingPhone)
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
            PickupSchedule schedule = loadPickupSchedule(order);
            return OrderResponse.from(order, items, schedule);
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
        PickupSchedule schedule = loadPickupSchedule(order);
        return OrderResponse.from(order, items, schedule);
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
            PickupSchedule schedule = loadPickupSchedule(order);
            return OrderResponse.from(order, items, schedule);
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
        PickupSchedule schedule = loadPickupSchedule(order);
        return OrderResponse.from(order, items, schedule);
    }

    /**
     * Handles non-cancel status transitions by the organizer:
     * PENDING → CONFIRMED, CONFIRMED → READY (individual), READY → COMPLETED (check-in).
     */
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
        PickupSchedule schedule = loadPickupSchedule(order);

        notifyCustomerInApp(order, newStatus);

        return OrderResponse.from(order, items, schedule);
    }

    // ------------------------------------------------------------------ //
    //  CHECK-IN (READY → COMPLETED)
    // ------------------------------------------------------------------ //

    @Transactional
    public OrderResponse checkInOrder(UUID ownerId, UUID orgId, UUID orderId) {
        Organization org = organizationService.getOwnOrganizationEntity(ownerId, orgId);

        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order", orderId.toString()));

        if (!org.getId().equals(order.getOrgId())) {
            throw new ResourceNotFoundException("Order", orderId.toString());
        }

        if (order.getStatus() != OrderStatus.READY) {
            throw new ValidationException(
                "Only READY orders can be checked in. Current status: " + order.getStatus()
            );
        }

        order.setStatus(OrderStatus.COMPLETED);
        order = orderRepository.save(order);

        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        PickupSchedule schedule = loadPickupSchedule(order);

        notifyCustomerInApp(order, OrderStatus.COMPLETED);

        return OrderResponse.from(order, items, schedule);
    }

    // ------------------------------------------------------------------ //
    //  PICKUP SCHEDULE MANAGEMENT
    // ------------------------------------------------------------------ //

    @Transactional
    public PickupScheduleResponse createPickupSchedule(UUID ownerId, UUID orgId, PickupScheduleRequest request) {
        Organization org = organizationService.getOwnOrganizationEntity(ownerId, orgId);

        // Validate all orders belong to this org and are CONFIRMED
        List<Order> orders = request.getOrderIds().stream()
            .map(id -> orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id.toString())))
            .toList();

        for (Order order : orders) {
            if (!org.getId().equals(order.getOrgId())) {
                throw new ValidationException("Order " + order.getId() + " does not belong to this organization.");
            }
            if (order.getStatus() != OrderStatus.CONFIRMED) {
                throw new ValidationException(
                    "Order " + order.getId() + " is not CONFIRMED (status: " + order.getStatus() + ")."
                );
            }
        }

        PickupSchedule schedule = pickupScheduleRepository.save(PickupSchedule.builder()
            .orgId(org.getId())
            .pickupDate(request.getPickupDate())
            .pickupTimeSlot(request.getPickupTimeSlot())
            .location(request.getLocation())
            .notes(request.getNotes())
            .build());

        // Move each order CONFIRMED → READY and link to schedule
        String pickupDateStr = request.getPickupDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        for (Order order : orders) {
            order.setStatus(OrderStatus.READY);
            order.setPickupScheduleId(schedule.getId());
            orderRepository.save(order);

            sendPickupNotification(order, schedule, pickupDateStr);
        }

        return PickupScheduleResponse.from(schedule, orders.size());
    }

    @Transactional(readOnly = true)
    public Page<PickupScheduleResponse> getPickupSchedules(UUID ownerId, UUID orgId, Pageable pageable) {
        Organization org = organizationService.getOwnOrganizationEntity(ownerId, orgId);
        return pickupScheduleRepository.findByOrgIdOrderByPickupDateDesc(org.getId(), pageable)
            .map(schedule -> {
                long count = orderRepository.countByPickupScheduleId(schedule.getId());
                return PickupScheduleResponse.from(schedule, (int) count);
            });
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getPickupScheduleOrders(UUID ownerId, UUID orgId, UUID scheduleId) {
        Organization org = organizationService.getOwnOrganizationEntity(ownerId, orgId);

        PickupSchedule schedule = pickupScheduleRepository.findById(scheduleId)
            .orElseThrow(() -> new ResourceNotFoundException("Pickup schedule", scheduleId.toString()));

        if (!org.getId().equals(schedule.getOrgId())) {
            throw new ResourceNotFoundException("Pickup schedule", scheduleId.toString());
        }

        return orderRepository.findByPickupScheduleId(scheduleId).stream()
            .map(order -> {
                List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
                return OrderResponse.from(order, items, schedule);
            })
            .toList();
    }

    // ------------------------------------------------------------------ //
    //  CUSTOMER CANCELLATION
    // ------------------------------------------------------------------ //

    @Transactional
    public OrderResponse cancelCustomerOrder(UUID userId, UUID orderId, CancelOrderRequest request) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order", orderId.toString()));

        if (!userId.equals(order.getUserId())) {
            throw new ResourceNotFoundException("Order", orderId.toString());
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new ValidationException(
                "Customers may only cancel PENDING orders. Current status: " + order.getStatus()
            );
        }

        applyCancel(order, "customer", request.getCancelReason(), request.getCancelReasonNote());
        order = orderRepository.save(order);

        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        restoreStockForItems(items);

        // Notify customer (confirmation)
        sendCancelEmail(order, "customer");

        // Notify org owner
        notifyOrgOwnerOfCancel(order);

        return OrderResponse.from(order, items);
    }

    // ------------------------------------------------------------------ //
    //  ORGANIZER CANCELLATION
    // ------------------------------------------------------------------ //

    @Transactional
    public OrderResponse cancelOrgOrder(UUID ownerId, UUID orgId, UUID orderId, CancelOrderRequest request) {
        Organization org = organizationService.getOwnOrganizationEntity(ownerId, orgId);

        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order", orderId.toString()));

        if (!org.getId().equals(order.getOrgId())) {
            throw new ResourceNotFoundException("Order", orderId.toString());
        }

        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new ValidationException(
                "Organizers may cancel PENDING or CONFIRMED orders. Current status: " + order.getStatus()
            );
        }

        applyCancel(order, "organizer", request.getCancelReason(), request.getCancelReasonNote());
        order = orderRepository.save(order);

        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        restoreStockForItems(items);

        // Notify customer
        sendCancelEmail(order, "organizer");
        notifyCustomerInApp(order, OrderStatus.CANCELLED);

        return OrderResponse.from(order, items);
    }

    // ------------------------------------------------------------------ //
    //  GUEST ORDER TRACKING
    // ------------------------------------------------------------------ //

    @Transactional(readOnly = true)
    public OrderResponse getGuestOrderByEmail(UUID orderId, String guestEmail) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order", orderId.toString()));

        if (order.getUserId() != null
                || order.getGuestEmail() == null
                || !order.getGuestEmail().equalsIgnoreCase(guestEmail.trim())) {
            throw new ResourceNotFoundException("Order", orderId.toString());
        }

        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        PickupSchedule schedule = loadPickupSchedule(order);
        return OrderResponse.from(order, items, schedule);
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
     * Valid non-cancel transitions (cancel is handled by dedicated cancel endpoints):
     * PENDING → CONFIRMED
     * CONFIRMED → READY   (individual; batch is via createPickupSchedule)
     * READY → COMPLETED   (used as fallback; preferred path is checkInOrder)
     */
    private void validateStatusTransition(OrderStatus current, OrderStatus next) {
        boolean valid = switch (current) {
            case PENDING -> next == OrderStatus.CONFIRMED;
            case CONFIRMED -> next == OrderStatus.READY;
            case READY -> next == OrderStatus.COMPLETED;
            case COMPLETED, CANCELLED -> false;
        };

        if (!valid) {
            throw new ValidationException(
                "Cannot transition order from " + current + " to " + next + "."
            );
        }
    }

    private void applyCancel(Order order, String cancelledBy, String cancelReason, String cancelReasonNote) {
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledBy(cancelledBy);
        order.setCancelReason(cancelReason);
        order.setCancelReasonNote(cancelReasonNote);
        order.setCancelledAt(LocalDateTime.now());
    }

    private void restoreStockForItems(List<OrderItem> items) {
        items.forEach(item -> merchItemRepository.restoreStock(item.getMerchId(), item.getQuantity()));
    }

    private PickupSchedule loadPickupSchedule(Order order) {
        if (order.getPickupScheduleId() == null) return null;
        return pickupScheduleRepository.findById(order.getPickupScheduleId()).orElse(null);
    }

    private void notifyCustomerInApp(Order order, OrderStatus status) {
        if (order.getUserId() == null) return;
        try {
            String email = userRepository.findById(order.getUserId())
                .map(u -> u.getEmail()).orElse(null);
            if (email != null) {
                emailService.sendOrderStatusUpdate(email, order.getId().toString(), status.name());
            }
            String shortId = order.getId().toString().substring(0, 8).toUpperCase();
            String title = switch (status) {
                case CONFIRMED  -> "Đơn hàng đã được xác nhận";
                case READY      -> "Đơn hàng sẵn sàng để nhận";
                case COMPLETED  -> "Đơn hàng đã hoàn thành";
                case CANCELLED  -> "Đơn hàng đã bị huỷ";
                default         -> "Cập nhật đơn hàng";
            };
            String message = switch (status) {
                case CONFIRMED -> "Đơn hàng #" + shortId + " đã được ban tổ chức xác nhận.";
                case READY     -> "Đơn hàng #" + shortId + " đã sẵn sàng. Hãy kiểm tra lịch nhận hàng.";
                case COMPLETED -> "Bạn đã nhận thành công đơn hàng #" + shortId + ".";
                case CANCELLED -> "Đơn hàng #" + shortId + " đã bị huỷ.";
                default        -> "Trạng thái đơn hàng #" + shortId + " đã thay đổi.";
            };
            NotificationType type = switch (status) {
                case CONFIRMED -> NotificationType.ORDER_CONFIRMED;
                case READY     -> NotificationType.ORDER_READY;
                case COMPLETED -> NotificationType.ORDER_COMPLETED;
                case CANCELLED -> NotificationType.ORDER_CANCELLED;
                default        -> NotificationType.ORDER_CONFIRMED;
            };
            notificationService.push(order.getUserId(), title, message, type, order.getId());
        } catch (Exception e) {
            log.warn("Failed to send customer notification for order {}: {}", order.getId(), e.getMessage());
        }
    }

    private void sendCancelEmail(Order order, String cancelledBy) {
        try {
            String email = resolveCustomerEmail(order);
            if (email != null) {
                emailService.sendOrderCancelledNotification(
                    email, order.getId().toString(),
                    order.getCancelReason(), cancelledBy
                );
            }
        } catch (Exception e) {
            log.warn("Failed to send cancel email for order {}: {}", order.getId(), e.getMessage());
        }
    }

    private void notifyOrgOwnerOfCancel(Order order) {
        try {
            Organization org = organizationService.getOrganizationEntityById(order.getOrgId());
            userRepository.findById(org.getOwnerId()).ifPresent(owner ->
                emailService.sendOrderCancelledNotification(
                    owner.getEmail(), order.getId().toString(),
                    order.getCancelReason(), "customer"
                )
            );
        } catch (Exception e) {
            log.warn("Failed to notify org owner for cancelled order {}: {}", order.getId(), e.getMessage());
        }
    }

    private void sendPickupNotification(Order order, PickupSchedule schedule, String pickupDateStr) {
        try {
            String email = resolveCustomerEmail(order);
            if (email != null) {
                emailService.sendPickupScheduleNotification(
                    email, order.getId().toString(),
                    pickupDateStr,
                    schedule.getPickupTimeSlot(),
                    schedule.getLocation(),
                    schedule.getNotes()
                );
            }
            if (order.getUserId() != null) {
                String shortId = order.getId().toString().substring(0, 8).toUpperCase();
                notificationService.push(
                    order.getUserId(),
                    "Lịch nhận hàng đã được tạo",
                    "Đơn hàng #" + shortId + " có thể nhận vào " + pickupDateStr
                        + " lúc " + schedule.getPickupTimeSlot()
                        + " tại " + schedule.getLocation() + ".",
                    NotificationType.PICKUP_SCHEDULED,
                    order.getId()
                );
            }
        } catch (Exception e) {
            log.warn("Failed to send pickup notification for order {}: {}", order.getId(), e.getMessage());
        }
    }

    private String resolveCustomerEmail(Order order) {
        if (order.getUserId() != null) {
            return userRepository.findById(order.getUserId()).map(u -> u.getEmail()).orElse(null);
        }
        return (order.getGuestEmail() != null && !order.getGuestEmail().isBlank())
            ? order.getGuestEmail() : null;
    }
}
