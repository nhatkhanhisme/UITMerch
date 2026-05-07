package com.uitmerch.backend.carts.repository;

import com.uitmerch.backend.auth.entity.User;
import com.uitmerch.backend.carts.entity.Cart;
import com.uitmerch.backend.common.model.CartStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for cart entities.
 */
@Repository
public interface CartRepository extends JpaRepository<Cart, UUID> {

    Optional<Cart> findByCustomerUserAndStatus(User customerUser, CartStatus status);

    List<Cart> findByCustomerUserAndStatusOrderByCreatedAtAsc(User customerUser, CartStatus status);
}