package com.uitmerch.backend.collections.service;

import com.uitmerch.backend.auth.entity.User;
import com.uitmerch.backend.collections.entity.Collection;
import com.uitmerch.backend.collections.repository.CollectionRepository;
import com.uitmerch.backend.common.exception.ResourceNotFoundException;
import com.uitmerch.backend.common.exception.ValidationException;
import com.uitmerch.backend.common.model.OrderStatus;
import com.uitmerch.backend.orders.entity.Order;
import com.uitmerch.backend.orders.entity.OrderItem;
import com.uitmerch.backend.orders.repository.OrderItemRepository;
import com.uitmerch.backend.orders.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Creates collection rows for successful orders.
 */
@Service
public class CollectionServiceImpl implements CollectionService {

    private final CollectionRepository collectionRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public CollectionServiceImpl(
        CollectionRepository collectionRepository,
        OrderRepository orderRepository,
        OrderItemRepository orderItemRepository
    ) {
        this.collectionRepository = collectionRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Override
    @Transactional
    public void createCollectionsForSuccessfulOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getStatus() != OrderStatus.SUCCESS) {
            throw new ValidationException("Collections can only be created for SUCCESS orders");
        }

        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
        Instant acquiredAt = order.getCompletedAt() != null ? order.getCompletedAt() : Instant.now();
        User customerUser = order.getCustomerUser();

        for (OrderItem orderItem : orderItems) {
            if (orderItem.getMerchItem() == null) {
                continue;
            }

            UUID merchItemId = orderItem.getMerchItem().getId();
            boolean alreadyCollected = collectionRepository.findByCustomerUserIdAndMerchItemId(customerUser.getId(), merchItemId)
                .isPresent();

            if (alreadyCollected) {
                continue;
            }

            Collection collection = Collection.builder()
                .customerUser(customerUser)
                .merchItem(orderItem.getMerchItem())
                .acquiredAt(acquiredAt)
                .build();
            collectionRepository.save(collection);
        }
    }
}