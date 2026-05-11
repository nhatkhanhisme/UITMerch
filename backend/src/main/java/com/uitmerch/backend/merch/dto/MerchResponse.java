package com.uitmerch.backend.merch.dto;

import com.uitmerch.backend.common.model.MerchItemStatus;
import com.uitmerch.backend.merch.entity.MerchItem;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    private MerchItemStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static MerchResponse from(MerchItem item) {
        return MerchResponse.builder()
            .id(item.getId())
            .orgId(item.getOrgId())
            .name(item.getName())
            .description(item.getDescription())
            .price(item.getPrice())
            .stock(item.getStock())
            .imageUrl(item.getImageUrl())
            .status(item.getStatus())
            .createdAt(item.getCreatedAt())
            .updatedAt(item.getUpdatedAt())
            .build();
    }
}
