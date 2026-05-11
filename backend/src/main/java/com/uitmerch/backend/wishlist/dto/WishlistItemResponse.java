package com.uitmerch.backend.wishlist.dto;

import com.uitmerch.backend.merch.dto.MerchResponse;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class WishlistItemResponse {

    private UUID id;
    private MerchResponse merch;
    private LocalDateTime addedAt;
}
