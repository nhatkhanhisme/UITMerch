package com.uitmerch.backend.wishlist.service;

import com.uitmerch.backend.common.exception.ConflictException;
import com.uitmerch.backend.common.exception.ResourceNotFoundException;
import com.uitmerch.backend.merch.entity.MerchItem;
import com.uitmerch.backend.merch.repository.MerchItemRepository;
import com.uitmerch.backend.wishlist.entity.Wishlist;
import com.uitmerch.backend.wishlist.entity.WishlistItem;
import com.uitmerch.backend.wishlist.repository.WishlistItemRepository;
import com.uitmerch.backend.wishlist.repository.WishlistRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WishlistServiceTest {

    @Mock private WishlistRepository wishlistRepository;
    @Mock private WishlistItemRepository wishlistItemRepository;
    @Mock private MerchItemRepository merchItemRepository;

    @InjectMocks private WishlistService wishlistService;

    private final UUID userId    = UUID.randomUUID();
    private final UUID wishlistId = UUID.randomUUID();
    private final UUID merchId   = UUID.randomUUID();

    private Wishlist wishlist() {
        return Wishlist.builder().id(wishlistId).userId(userId).build();
    }

    private MerchItem merch() {
        return MerchItem.builder().id(merchId).name("T-shirt")
            .price(BigDecimal.valueOf(50_000)).stock(10).build();
    }

    // ── getWishlist ──────────────────────────────────────────────────────────

    @Test
    void getWishlist_existingWishlist_returnsItems() {
        WishlistItem item = WishlistItem.builder()
            .id(UUID.randomUUID()).wishlistId(wishlistId).merchId(merchId).build();

        when(wishlistRepository.findByUserId(userId)).thenReturn(Optional.of(wishlist()));
        when(wishlistItemRepository.findByWishlistId(wishlistId)).thenReturn(List.of(item));
        when(merchItemRepository.findAllById(List.of(merchId))).thenReturn(List.of(merch()));

        var response = wishlistService.getWishlist(userId);

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getId()).isEqualTo(wishlistId);
    }

    @Test
    void getWishlist_noWishlistYet_createsEmptyWishlist() {
        when(wishlistRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(wishlistRepository.save(any())).thenReturn(wishlist());
        when(wishlistItemRepository.findByWishlistId(wishlistId)).thenReturn(Collections.emptyList());
        when(merchItemRepository.findAllById(any())).thenReturn(Collections.emptyList());

        var response = wishlistService.getWishlist(userId);

        assertThat(response.getItems()).isEmpty();
        verify(wishlistRepository).save(any());
    }

    // ── addItem ──────────────────────────────────────────────────────────────

    @Test
    void addItem_success_savesItemAndReturnsWishlist() {
        when(merchItemRepository.existsById(merchId)).thenReturn(true);
        when(wishlistRepository.findByUserId(userId)).thenReturn(Optional.of(wishlist()));
        when(wishlistItemRepository.existsByWishlistIdAndMerchId(wishlistId, merchId)).thenReturn(false);
        when(wishlistItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(wishlistItemRepository.findByWishlistId(wishlistId)).thenReturn(Collections.emptyList());
        when(merchItemRepository.findAllById(any())).thenReturn(Collections.emptyList());

        wishlistService.addItem(userId, merchId);

        verify(wishlistItemRepository).save(any());
    }

    @Test
    void addItem_merchNotFound_throwsResourceNotFound() {
        when(merchItemRepository.existsById(merchId)).thenReturn(false);

        assertThatThrownBy(() -> wishlistService.addItem(userId, merchId))
            .isInstanceOf(ResourceNotFoundException.class);
        verify(wishlistItemRepository, never()).save(any());
    }

    @Test
    void addItem_alreadyInWishlist_throwsConflict() {
        when(merchItemRepository.existsById(merchId)).thenReturn(true);
        when(wishlistRepository.findByUserId(userId)).thenReturn(Optional.of(wishlist()));
        when(wishlistItemRepository.existsByWishlistIdAndMerchId(wishlistId, merchId)).thenReturn(true);

        assertThatThrownBy(() -> wishlistService.addItem(userId, merchId))
            .isInstanceOf(ConflictException.class);
        verify(wishlistItemRepository, never()).save(any());
    }

    // ── removeItem ───────────────────────────────────────────────────────────

    @Test
    void removeItem_success_deletesItem() {
        when(wishlistRepository.findByUserId(userId)).thenReturn(Optional.of(wishlist()));
        when(wishlistItemRepository.existsByWishlistIdAndMerchId(wishlistId, merchId)).thenReturn(true);

        wishlistService.removeItem(userId, merchId);

        verify(wishlistItemRepository).deleteByWishlistIdAndMerchId(wishlistId, merchId);
    }

    @Test
    void removeItem_itemNotInWishlist_throwsResourceNotFound() {
        when(wishlistRepository.findByUserId(userId)).thenReturn(Optional.of(wishlist()));
        when(wishlistItemRepository.existsByWishlistIdAndMerchId(wishlistId, merchId)).thenReturn(false);

        assertThatThrownBy(() -> wishlistService.removeItem(userId, merchId))
            .isInstanceOf(ResourceNotFoundException.class);
        verify(wishlistItemRepository, never()).deleteByWishlistIdAndMerchId(any(), any());
    }
}
