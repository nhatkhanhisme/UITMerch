package com.uitmerch.backend.wishlist.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class WishlistResponse {

    private UUID id;
    private List<WishlistItemResponse> items;
}
