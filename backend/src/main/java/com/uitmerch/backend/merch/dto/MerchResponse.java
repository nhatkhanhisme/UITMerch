package com.uitmerch.backend.merch.dto;

import com.uitmerch.backend.common.model.MerchItemStatus;
import com.uitmerch.backend.merch.entity.Category;
import com.uitmerch.backend.merch.entity.MerchItem;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class MerchResponse {

    private UUID id;
    private UUID orgId;
    private String name;
    private String description;
    private BigDecimal price;
    private int stock;
    private String imageUrl;
    private List<String> images;
    private MerchItemStatus status;
    private UUID categoryId;
    private String categorySlug;
    private String categoryName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static MerchResponse from(MerchItem item) {
        return from(item, null, List.of());
    }

    public static MerchResponse from(MerchItem item, Category category) {
        return from(item, category, List.of());
    }

    public static MerchResponse from(MerchItem item, Category category, List<String> images) {
        // Backward compat: if no images in the new table, fall back to the legacy imageUrl column.
        List<String> resolvedImages = (images != null && !images.isEmpty())
            ? images
            : (item.getImageUrl() != null ? List.of(item.getImageUrl()) : List.of());

        return MerchResponse.builder()
            .id(item.getId())
            .orgId(item.getOrgId())
            .name(item.getName())
            .description(item.getDescription())
            .price(item.getPrice())
            .stock(item.getStock())
            .imageUrl(resolvedImages.isEmpty() ? null : resolvedImages.get(0))
            .images(resolvedImages)
            .status(item.getStatus())
            .categoryId(category != null ? category.getId() : item.getCategoryId())
            .categorySlug(category != null ? category.getSlug() : null)
            .categoryName(category != null ? category.getName() : null)
            .createdAt(item.getCreatedAt())
            .updatedAt(item.getUpdatedAt())
            .build();
    }
}
