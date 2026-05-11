package com.uitmerch.backend.cart.repository;

import com.uitmerch.backend.cart.entity.Cart;
import com.uitmerch.backend.common.model.CartStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartRepository extends JpaRepository<Cart, UUID> {

    List<Cart> findByUserId(UUID userId);

    Optional<Cart> findByUserIdAndStatus(UUID userId, CartStatus status);
}
