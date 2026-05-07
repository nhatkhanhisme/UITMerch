package com.uitmerch.backend.orders.repository;

import com.uitmerch.backend.auth.entity.User;
import com.uitmerch.backend.common.model.OrderStatus;
import com.uitmerch.backend.orders.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for order entities.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByIdempotencyKey(String idempotencyKey);

    Optional<Order> findByIdAndCustomerUser(UUID id, User customerUser);

    boolean existsByOrderCode(String orderCode);

    long countByCustomerUserAndStatus(User customerUser, OrderStatus status);
}