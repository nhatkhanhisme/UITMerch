package com.uitmerch.backend.merch.dto;

import com.uitmerch.backend.merch.entity.Category;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class CategoryResponse {

    private UUID id;
    private String slug;
    private String name;
    private String description;
    private int displayOrder;

    public static CategoryResponse from(Category category) {
        return CategoryResponse.builder()
            .id(category.getId())
            .slug(category.getSlug())
            .name(category.getName())
            .description(category.getDescription())
            .displayOrder(category.getDisplayOrder())
            .build();
    }
}
