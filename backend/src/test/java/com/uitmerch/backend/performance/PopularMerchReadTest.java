package com.uitmerch.backend.performance;

import com.uitmerch.backend.ai.service.MerchEmbeddingService;
import com.uitmerch.backend.following.PublicationService;
import com.uitmerch.backend.merch.entity.MerchItem;
import com.uitmerch.backend.merch.repository.CategoryRepository;
import com.uitmerch.backend.merch.repository.MerchImageRepository;
import com.uitmerch.backend.merch.repository.MerchItemRepository;
import com.uitmerch.backend.merch.service.MerchService;
import com.uitmerch.backend.order.repository.OrderItemRepository;
import com.uitmerch.backend.organization.service.OrganizationService;
import com.uitmerch.backend.restock.RestockService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import com.uitmerch.backend.common.model.MerchItemStatus;

@ExtendWith(MockitoExtension.class)
class PopularMerchReadTest {
    @Mock MerchItemRepository merchandise;
    @Mock MerchImageRepository images;
    @Mock CategoryRepository categories;
    @Mock OrganizationService organizations;
    @Mock OrderItemRepository orderItems;
    @Mock MerchEmbeddingService embeddings;
    @Mock RestockService restock;
    @Mock PublicationService publications;
    @InjectMocks MerchService service;
    @Captor ArgumentCaptor<List<UUID>> imageIds;

    @Test
    void onlyLoadsImagesForTheTenRankedResults() {
        var products = IntStream.range(0, 50).mapToObj(i -> MerchItem.builder().id(UUID.randomUUID())
            .name("Product " + i).stock(10).status(MerchItemStatus.PUBLISHED)
            .createdAt(LocalDateTime.now().minusDays(i)).build()).toList();
        when(merchandise.findAllByStatus(eq(MerchItemStatus.PUBLISHED), any())).thenReturn(new PageImpl<>(products));
        when(orderItems.sumQuantityByMerchIds(any())).thenReturn(List.of());
        when(orderItems.sumQuantityByMerchIdsSince(any(), any())).thenReturn(List.of());
        when(categories.findAll()).thenReturn(List.of());
        var result = service.getPopularMerch();
        verify(images).findByMerchIdInOrderByPosition(imageIds.capture());
        assertThat(result).hasSize(10);
        assertThat(imageIds.getValue()).containsExactlyElementsOf(result.stream().map(value -> value.getId()).toList());
    }

    @Test
    void anEmptyCatalogDoesNotReadImagesOrAggregates() {
        when(merchandise.findAllByStatus(eq(MerchItemStatus.PUBLISHED), any())).thenReturn(new PageImpl<>(List.of()));
        assertThat(service.getPopularMerch()).isEmpty();
        verifyNoInteractions(images, orderItems, categories);
    }
}
