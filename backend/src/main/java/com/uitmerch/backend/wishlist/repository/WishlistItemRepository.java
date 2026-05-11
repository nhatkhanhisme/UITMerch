package com.uitmerch.backend.wishlist.repository;

import com.uitmerch.backend.wishlist.entity.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WishlistItemRepository extends JpaRepository<WishlistItem, UUID> {

    List<WishlistItem> findByWishlistId(UUID wishlistId);

    Optional<WishlistItem> findByWishlistIdAndMerchId(UUID wishlistId, UUID merchId);

    boolean existsByWishlistIdAndMerchId(UUID wishlistId, UUID merchId);

    void deleteByWishlistIdAndMerchId(UUID wishlistId, UUID merchId);
}
