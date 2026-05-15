package com.uitmerch.backend.wishlist.service;

import com.uitmerch.backend.common.exception.ConflictException;
import com.uitmerch.backend.common.exception.ResourceNotFoundException;
import com.uitmerch.backend.merch.dto.MerchResponse;
import com.uitmerch.backend.merch.entity.MerchItem;
import com.uitmerch.backend.merch.repository.MerchItemRepository;
import com.uitmerch.backend.wishlist.dto.WishlistItemResponse;
import com.uitmerch.backend.wishlist.dto.WishlistResponse;
import com.uitmerch.backend.wishlist.entity.Wishlist;
import com.uitmerch.backend.wishlist.entity.WishlistItem;
import com.uitmerch.backend.wishlist.repository.WishlistItemRepository;
import com.uitmerch.backend.wishlist.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final WishlistItemRepository wishlistItemRepository;
    private final MerchItemRepository merchItemRepository;

    @Transactional(readOnly = true)
    public WishlistResponse getWishlist(UUID userId) {
        Wishlist wishlist = findOrCreate(userId);
        List<WishlistItem> items = wishlistItemRepository.findByWishlistId(wishlist.getId());

        // Batch-fetch all referenced merch items in one query
        List<UUID> merchIds = items.stream().map(WishlistItem::getMerchId).toList();
        Map<UUID, MerchItem> merchById = merchItemRepository.findAllById(merchIds)
            .stream().collect(Collectors.toMap(MerchItem::getId, Function.identity()));

        List<WishlistItemResponse> itemResponses = items.stream()
            .map(item -> WishlistItemResponse.builder()
                .id(item.getId())
                .merch(MerchResponse.from(merchById.get(item.getMerchId())))
                .addedAt(item.getCreatedAt())
                .build())
            .toList();

        return WishlistResponse.builder()
            .id(wishlist.getId())
            .items(itemResponses)
            .build();
    }

    @Transactional
    public WishlistResponse addItem(UUID userId, UUID merchId) {
        if (!merchItemRepository.existsById(merchId)) {
            throw new ResourceNotFoundException("Merch item", merchId.toString());
        }

        Wishlist wishlist = findOrCreate(userId);

        if (wishlistItemRepository.existsByWishlistIdAndMerchId(wishlist.getId(), merchId)) {
            throw new ConflictException("Merch item is already in your wishlist.");
        }

        wishlistItemRepository.save(WishlistItem.builder()
            .wishlistId(wishlist.getId())
            .merchId(merchId)
            .build());

        return getWishlist(userId);
    }

    @Transactional
    public void removeItem(UUID userId, UUID merchId) {
        Wishlist wishlist = findOrCreate(userId);
        if (!wishlistItemRepository.existsByWishlistIdAndMerchId(wishlist.getId(), merchId)) {
            throw new ResourceNotFoundException("Merch item is not in your wishlist.");
        }
        wishlistItemRepository.deleteByWishlistIdAndMerchId(wishlist.getId(), merchId);
    }

    private Wishlist findOrCreate(UUID userId) {
        return wishlistRepository.findByUserId(userId)
            .orElseGet(() -> wishlistRepository.save(
                Wishlist.builder().userId(userId).build()
            ));
    }
}
