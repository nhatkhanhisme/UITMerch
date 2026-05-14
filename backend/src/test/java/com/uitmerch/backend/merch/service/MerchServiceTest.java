package com.uitmerch.backend.merch.service;

import com.uitmerch.backend.common.exception.ResourceNotFoundException;
import com.uitmerch.backend.common.exception.ValidationException;
import com.uitmerch.backend.common.model.MerchItemStatus;
import com.uitmerch.backend.common.model.OrganizationStatus;
import com.uitmerch.backend.merch.dto.CreateMerchRequest;
import com.uitmerch.backend.merch.dto.MerchResponse;
import com.uitmerch.backend.merch.dto.UpdateMerchRequest;
import com.uitmerch.backend.merch.entity.Category;
import com.uitmerch.backend.merch.entity.MerchItem;
import com.uitmerch.backend.merch.repository.CategoryRepository;
import com.uitmerch.backend.merch.repository.MerchImageRepository;
import com.uitmerch.backend.merch.repository.MerchItemRepository;
import com.uitmerch.backend.order.repository.OrderItemRepository;
import com.uitmerch.backend.organization.entity.Organization;
import com.uitmerch.backend.organization.service.OrganizationService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchServiceTest {

    @Mock private MerchItemRepository merchItemRepository;
    @Mock private MerchImageRepository merchImageRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private OrganizationService organizationService;
    @Mock private OrderItemRepository orderItemRepository;

    @InjectMocks private MerchService merchService;

    private final UUID ownerId = UUID.randomUUID();
    private final UUID orgId = UUID.randomUUID();
    private final UUID merchId = UUID.randomUUID();

    private Organization activeOrg() {
        return Organization.builder().id(orgId).ownerId(ownerId).status(OrganizationStatus.ACTIVE).build();
    }

    private Organization pendingOrg() {
        return Organization.builder().id(orgId).ownerId(ownerId).status(OrganizationStatus.PENDING).build();
    }

    private MerchItem savedItem() {
        return MerchItem.builder()
            .id(merchId)
            .orgId(orgId)
            .name("Test Merch")
            .description("desc")
            .price(BigDecimal.valueOf(75_000))
            .stock(10)
            .status(MerchItemStatus.DRAFT)
            .build();
    }

    // ── createMerch ───────────────────────────────────────────────────────────

    @Test
    void createMerch_activeOrg_noCategory_succeeds() {
        when(organizationService.getOwnOrganizationEntity(ownerId, orgId)).thenReturn(activeOrg());
        when(merchItemRepository.save(any())).thenReturn(savedItem());

        CreateMerchRequest req = new CreateMerchRequest();
        req.setName("Test Merch");
        req.setPrice(BigDecimal.valueOf(75_000));
        req.setStock(10);

        MerchResponse response = merchService.createMerch(ownerId, orgId, req);

        assertThat(response.getOrgId()).isEqualTo(orgId);
        verify(merchItemRepository).save(any());
    }

    @Test
    void createMerch_activeOrg_withCategory_resolvesCategorySlug() {
        Category cat = Category.builder()
            .id(UUID.randomUUID())
            .slug("shirt")
            .name("Shirt")
            .build();

        when(organizationService.getOwnOrganizationEntity(ownerId, orgId)).thenReturn(activeOrg());
        when(categoryRepository.findBySlug("shirt")).thenReturn(Optional.of(cat));
        when(merchItemRepository.save(any())).thenReturn(savedItem());

        CreateMerchRequest req = new CreateMerchRequest();
        req.setName("Test Merch");
        req.setPrice(BigDecimal.valueOf(75_000));
        req.setStock(10);
        req.setCategorySlug("shirt");

        MerchResponse response = merchService.createMerch(ownerId, orgId, req);

        assertThat(response).isNotNull();
        verify(categoryRepository).findBySlug("shirt");
    }

    @Test
    void createMerch_inactiveOrg_throwsValidation() {
        when(organizationService.getOwnOrganizationEntity(ownerId, orgId)).thenReturn(pendingOrg());

        CreateMerchRequest req = new CreateMerchRequest();
        req.setName("Test Merch");
        req.setPrice(BigDecimal.valueOf(75_000));
        req.setStock(10);

        assertThatThrownBy(() -> merchService.createMerch(ownerId, orgId, req))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("ACTIVE");
    }

    @Test
    void createMerch_invalidCategorySlug_throwsResourceNotFound() {
        when(organizationService.getOwnOrganizationEntity(ownerId, orgId)).thenReturn(activeOrg());
        when(categoryRepository.findBySlug("unknown")).thenReturn(Optional.empty());

        CreateMerchRequest req = new CreateMerchRequest();
        req.setName("Test Merch");
        req.setPrice(BigDecimal.valueOf(75_000));
        req.setStock(10);
        req.setCategorySlug("unknown");

        assertThatThrownBy(() -> merchService.createMerch(ownerId, orgId, req))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── updateMerch ───────────────────────────────────────────────────────────

    @Test
    void updateMerch_publishWithInactiveOrg_throwsValidation() {
        when(organizationService.getOwnOrganizationEntity(ownerId, orgId)).thenReturn(pendingOrg());
        when(merchItemRepository.findByIdAndOrgId(merchId, orgId)).thenReturn(Optional.of(savedItem()));

        UpdateMerchRequest req = new UpdateMerchRequest();
        req.setStatus(MerchItemStatus.PUBLISHED);

        assertThatThrownBy(() -> merchService.updateMerch(ownerId, orgId, merchId, req))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("ACTIVE");
    }

    @Test
    void updateMerch_publishWithActiveOrg_succeeds() {
        MerchItem item = savedItem();
        when(organizationService.getOwnOrganizationEntity(ownerId, orgId)).thenReturn(activeOrg());
        when(merchItemRepository.findByIdAndOrgId(merchId, orgId)).thenReturn(Optional.of(item));
        when(merchItemRepository.save(any())).thenReturn(item);
        when(merchImageRepository.findByMerchIdOrderByPosition(any())).thenReturn(Collections.emptyList());

        UpdateMerchRequest req = new UpdateMerchRequest();
        req.setStatus(MerchItemStatus.PUBLISHED);

        MerchResponse response = merchService.updateMerch(ownerId, orgId, merchId, req);

        assertThat(response).isNotNull();
        verify(merchItemRepository).save(item);
    }

    @Test
    void updateMerch_partialUpdate_onlyModifiesProvidedFields() {
        MerchItem item = savedItem();
        when(organizationService.getOwnOrganizationEntity(ownerId, orgId)).thenReturn(activeOrg());
        when(merchItemRepository.findByIdAndOrgId(merchId, orgId)).thenReturn(Optional.of(item));
        when(merchItemRepository.save(any())).thenReturn(item);
        when(merchImageRepository.findByMerchIdOrderByPosition(any())).thenReturn(Collections.emptyList());

        UpdateMerchRequest req = new UpdateMerchRequest();
        req.setName("New Name");
        // price and stock not set — should remain unchanged

        merchService.updateMerch(ownerId, orgId, merchId, req);

        assertThat(item.getName()).isEqualTo("New Name");
        assertThat(item.getPrice()).isEqualTo(BigDecimal.valueOf(75_000));
    }

    @Test
    void updateMerch_itemNotOwned_throwsResourceNotFound() {
        when(organizationService.getOwnOrganizationEntity(ownerId, orgId)).thenReturn(activeOrg());
        when(merchItemRepository.findByIdAndOrgId(merchId, orgId)).thenReturn(Optional.empty());

        UpdateMerchRequest req = new UpdateMerchRequest();
        req.setName("New Name");

        assertThatThrownBy(() -> merchService.updateMerch(ownerId, orgId, merchId, req))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── deleteMerch ───────────────────────────────────────────────────────────

    @Test
    void deleteMerch_success_archivesItem() {
        MerchItem item = savedItem();
        when(organizationService.getOwnOrganizationEntity(ownerId, orgId)).thenReturn(activeOrg());
        when(merchItemRepository.findByIdAndOrgId(merchId, orgId)).thenReturn(Optional.of(item));
        when(merchItemRepository.save(any())).thenReturn(item);

        merchService.deleteMerch(ownerId, orgId, merchId);

        assertThat(item.getStatus()).isEqualTo(MerchItemStatus.ARCHIVED);
        verify(merchItemRepository).save(item);
    }

    @Test
    void deleteMerch_itemNotOwned_throwsResourceNotFound() {
        when(organizationService.getOwnOrganizationEntity(ownerId, orgId)).thenReturn(activeOrg());
        when(merchItemRepository.findByIdAndOrgId(merchId, orgId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> merchService.deleteMerch(ownerId, orgId, merchId))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getPublishedMerch ─────────────────────────────────────────────────────

    @Test
    void getPublishedMerch_publishedItem_returnsResponse() {
        MerchItem item = savedItem();
        item.setStatus(MerchItemStatus.PUBLISHED);

        when(merchItemRepository.findById(merchId)).thenReturn(Optional.of(item));
        when(merchImageRepository.findByMerchIdOrderByPosition(any())).thenReturn(Collections.emptyList());

        MerchResponse response = merchService.getPublishedMerch(merchId);

        assertThat(response.getId()).isEqualTo(merchId);
    }

    @Test
    void getPublishedMerch_draftItem_throwsResourceNotFound() {
        when(merchItemRepository.findById(merchId)).thenReturn(Optional.of(savedItem())); // DRAFT status

        assertThatThrownBy(() -> merchService.getPublishedMerch(merchId))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getPublishedMerch_notFound_throwsResourceNotFound() {
        when(merchItemRepository.findById(merchId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> merchService.getPublishedMerch(merchId))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getPopularMerch ───────────────────────────────────────────────────────

    @Test
    void getPopularMerch_noPublishedItems_returnsEmptyList() {
        when(merchItemRepository.findAllByStatus(MerchItemStatus.PUBLISHED)).thenReturn(Collections.emptyList());

        List<MerchResponse> result = merchService.getPopularMerch();

        assertThat(result).isEmpty();
    }

    @Test
    void getPopularMerch_withItems_returnsAtMostTen() {
        List<MerchItem> items = new java.util.ArrayList<>();
        for (int i = 0; i < 15; i++) {
            MerchItem m = MerchItem.builder()
                .id(UUID.randomUUID())
                .orgId(orgId)
                .name("Item " + i)
                .price(BigDecimal.valueOf(10_000))
                .stock(5)
                .status(MerchItemStatus.PUBLISHED)
                .build();
            // createdAt must be non-null for the popularity score calculation
            m.setCreatedAt(java.time.LocalDateTime.now().minusDays(i));
            items.add(m);
        }

        when(merchItemRepository.findAllByStatus(MerchItemStatus.PUBLISHED)).thenReturn(items);
        when(orderItemRepository.sumQuantityByMerchIds(any())).thenReturn(Collections.emptyList());
        when(orderItemRepository.sumQuantityByMerchIdsSince(any(), any())).thenReturn(Collections.emptyList());
        when(categoryRepository.findAll()).thenReturn(Collections.emptyList());
        when(merchImageRepository.findByMerchIdInOrderByPosition(any())).thenReturn(Collections.emptyList());

        List<MerchResponse> result = merchService.getPopularMerch();

        assertThat(result).hasSize(10);
    }
}
