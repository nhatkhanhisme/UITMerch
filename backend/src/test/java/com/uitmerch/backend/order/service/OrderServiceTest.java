package com.uitmerch.backend.order.service;

import com.uitmerch.backend.common.exception.ResourceNotFoundException;
import com.uitmerch.backend.common.exception.ValidationException;
import com.uitmerch.backend.common.model.MerchItemStatus;
import com.uitmerch.backend.common.model.OrderStatus;
import com.uitmerch.backend.merch.entity.MerchItem;
import com.uitmerch.backend.merch.repository.MerchItemRepository;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private MerchItemRepository merchItemRepository;
    @Mock private OrganizationService organizationService;

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
    void createInstantOrder_success_deductsStock() {
        MerchItem merch = publishedMerch(5);
        Order savedOrder = orderWithStatus(OrderStatus.PENDING);
        OrderItem savedItem = OrderItem.builder()
            .id(UUID.randomUUID())
            .orderId(orderId)
            .merchId(merchId)
            .merchName("Test Merch")
            .unitPrice(BigDecimal.valueOf(100_000))
            .quantity(2)
            .subtotal(BigDecimal.valueOf(200_000))
            .build();

        when(merchItemRepository.findById(merchId)).thenReturn(Optional.of(merch));
        when(orderRepository.save(any())).thenReturn(savedOrder);
        when(orderItemRepository.save(any())).thenReturn(savedItem);
        when(merchItemRepository.save(any())).thenReturn(merch);

        InstantOrderRequest req = new InstantOrderRequest();
        req.setMerchId(merchId);
        req.setQuantity(2);

        OrderResponse response = orderService.createInstantOrder(userId, req);

        assertThat(response.getOrgId()).isEqualTo(orgId);
        assertThat(merch.getStock()).isEqualTo(3); // 5 - 2
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
}
