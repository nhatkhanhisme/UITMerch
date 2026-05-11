package com.uitmerch.backend.cart.repository;

import com.uitmerch.backend.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    List<CartItem> findByCartId(UUID cartId);

    Optional<CartItem> findByCartIdAndMerchId(UUID cartId, UUID merchId);

    boolean existsByCartIdAndMerchId(UUID cartId, UUID merchId);

    void deleteByCartIdAndMerchId(UUID cartId, UUID merchId);
}
