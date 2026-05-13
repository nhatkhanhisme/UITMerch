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
import com.uitmerch.backend.merch.repository.MerchItemRepository;
import com.uitmerch.backend.order.repository.OrderItemRepository;
import com.uitmerch.backend.organization.entity.Organization;
import com.uitmerch.backend.organization.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MerchService {

    private final MerchItemRepository merchItemRepository;
    private final CategoryRepository categoryRepository;
    private final OrganizationService organizationService;
    private final OrderItemRepository orderItemRepository;

    // ------------------------------------------------------------------ //
    //  ORGANIZER
    // ------------------------------------------------------------------ //

    @Transactional
    public MerchResponse createMerch(UUID ownerId, CreateMerchRequest request) {
        Organization org = organizationService.getOwnOrganizationEntity(ownerId);
        if (org.getStatus() != OrganizationStatus.ACTIVE) {
            throw new ValidationException("Your organization must be ACTIVE to create merch. Current status: " + org.getStatus());
        }

        Category category = resolveCategory(request.getCategorySlug());

        MerchItem item = MerchItem.builder()
            .orgId(org.getId())
            .name(request.getName())
            .description(request.getDescription())
            .price(request.getPrice())
            .stock(request.getStock())
            .imageUrl(request.getImageUrl())
            .categoryId(category != null ? category.getId() : null)
            .build();

        return MerchResponse.from(merchItemRepository.save(item), category);
    }

    public Page<MerchResponse> getOwnMerch(UUID ownerId, Pageable pageable) {
        Organization org = organizationService.getOwnOrganizationEntity(ownerId);
        Map<UUID, Category> categoryMap = buildCategoryMap();
        return merchItemRepository.findByOrgId(org.getId(), pageable)
            .map(item -> MerchResponse.from(item, categoryMap.get(item.getCategoryId())));
    }

    public MerchResponse getOwnMerchItem(UUID ownerId, UUID merchId) {
        Organization org = organizationService.getOwnOrganizationEntity(ownerId);
        MerchItem item = findOwnItemOrThrow(org.getId(), merchId);
        Category category = item.getCategoryId() != null ? categoryRepository.findById(item.getCategoryId()).orElse(null) : null;
        return MerchResponse.from(item, category);
    }

    @Transactional
    public MerchResponse updateMerch(UUID ownerId, UUID merchId, UpdateMerchRequest request) {
        Organization org = organizationService.getOwnOrganizationEntity(ownerId);
        MerchItem item = findOwnItemOrThrow(org.getId(), merchId);

        if (request.getName() != null && !request.getName().isBlank()) {
            item.setName(request.getName());
        }
        if (request.getDescription() != null) {
            item.setDescription(request.getDescription());
        }
        if (request.getPrice() != null) {
            item.setPrice(request.getPrice());
        }
        if (request.getStock() != null) {
            item.setStock(request.getStock());
        }
        if (request.getImageUrl() != null) {
            item.setImageUrl(request.getImageUrl());
        }
        if (request.getStatus() != null) {
            if (request.getStatus() == MerchItemStatus.PUBLISHED && org.getStatus() != OrganizationStatus.ACTIVE) {
                throw new ValidationException("Your organization must be ACTIVE to publish merch.");
            }
            item.setStatus(request.getStatus());
        }

        Category category = null;
        if (request.getCategorySlug() != null) {
            category = resolveCategory(request.getCategorySlug());
            item.setCategoryId(category != null ? category.getId() : null);
        } else if (item.getCategoryId() != null) {
            category = categoryRepository.findById(item.getCategoryId()).orElse(null);
        }

        return MerchResponse.from(merchItemRepository.save(item), category);
    }

    @Transactional
    public void deleteMerch(UUID ownerId, UUID merchId) {
        Organization org = organizationService.getOwnOrganizationEntity(ownerId);
        MerchItem item = findOwnItemOrThrow(org.getId(), merchId);
        item.setStatus(MerchItemStatus.ARCHIVED);
        merchItemRepository.save(item);
    }

    // ------------------------------------------------------------------ //
    //  PUBLIC
    // ------------------------------------------------------------------ //

    public Page<MerchResponse> listPublished(String keyword, String categorySlug, Pageable pageable) {
        Map<UUID, Category> categoryMap = buildCategoryMap();

        if (categorySlug != null && !categorySlug.isBlank()) {
            Category category = categoryRepository.findBySlug(categorySlug.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Category", categorySlug));
            UUID catId = category.getId();

            if (keyword != null && !keyword.isBlank()) {
                return merchItemRepository
                    .findByStatusAndCategoryIdAndNameContainingIgnoreCase(MerchItemStatus.PUBLISHED, catId, keyword.trim(), pageable)
                    .map(item -> MerchResponse.from(item, categoryMap.get(item.getCategoryId())));
            }
            return merchItemRepository
                .findByStatusAndCategoryId(MerchItemStatus.PUBLISHED, catId, pageable)
                .map(item -> MerchResponse.from(item, categoryMap.get(item.getCategoryId())));
        }

        if (keyword != null && !keyword.isBlank()) {
            return merchItemRepository
                .findByStatusAndNameContainingIgnoreCase(MerchItemStatus.PUBLISHED, keyword.trim(), pageable)
                .map(item -> MerchResponse.from(item, categoryMap.get(item.getCategoryId())));
        }
        return merchItemRepository.findByStatus(MerchItemStatus.PUBLISHED, pageable)
            .map(item -> MerchResponse.from(item, categoryMap.get(item.getCategoryId())));
    }

    public MerchResponse getPublishedMerch(UUID merchId) {
        MerchItem item = merchItemRepository.findById(merchId)
            .filter(m -> m.getStatus() == MerchItemStatus.PUBLISHED)
            .orElseThrow(() -> new ResourceNotFoundException("Merch item", merchId.toString()));
        Category category = item.getCategoryId() != null ? categoryRepository.findById(item.getCategoryId()).orElse(null) : null;
        return MerchResponse.from(item, category);
    }

    public List<MerchResponse> getPopularMerch() {
        List<MerchItem> published = merchItemRepository.findAllByStatus(MerchItemStatus.PUBLISHED);
        if (published.isEmpty()) return List.of();

        List<UUID> ids = published.stream().map(MerchItem::getId).toList();
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        Map<UUID, Long> allTimeOrders = toOrderCountMap(orderItemRepository.sumQuantityByMerchIds(ids));
        Map<UUID, Long> recentOrders  = toOrderCountMap(orderItemRepository.sumQuantityByMerchIdsSince(ids, thirtyDaysAgo));

        Map<UUID, Category> categoryMap = buildCategoryMap();
        LocalDateTime now = LocalDateTime.now();

        return published.stream()
            .sorted(Comparator.comparingDouble(item -> -popularityScore(item, allTimeOrders, recentOrders, now)))
            .limit(10)
            .map(item -> MerchResponse.from(item, categoryMap.get(item.getCategoryId())))
            .toList();
    }

    // score = allTimeOrders + (recentOrders × 2) + newItemBonus
    // newItemBonus: items created in the last 30 days get up to 15 extra points so new items can surface alongside established ones
    private static double popularityScore(MerchItem item, Map<UUID, Long> allTime, Map<UUID, Long> recent, LocalDateTime now) {
        double allTimeCount = allTime.getOrDefault(item.getId(), 0L);
        double recentCount  = recent.getOrDefault(item.getId(), 0L);
        long   daysOld      = ChronoUnit.DAYS.between(item.getCreatedAt(), now);
        double newItemBonus = Math.max(0.0, 30.0 - daysOld) * 0.5;
        return allTimeCount + (recentCount * 2.0) + newItemBonus;
    }

    private static Map<UUID, Long> toOrderCountMap(List<Object[]> rows) {
        return rows.stream().collect(Collectors.toMap(
            row -> (UUID) row[0],
            row -> ((Number) row[1]).longValue()
        ));
    }

    public Page<MerchResponse> listByOrganization(UUID orgId, Pageable pageable) {
        Map<UUID, Category> categoryMap = buildCategoryMap();
        return merchItemRepository
            .findByOrgIdAndStatus(orgId, MerchItemStatus.PUBLISHED, pageable)
            .map(item -> MerchResponse.from(item, categoryMap.get(item.getCategoryId())));
    }

    // ------------------------------------------------------------------ //
    //  PACKAGE-INTERNAL (used by order module for stock deduction)
    // ------------------------------------------------------------------ //

    public MerchItem getMerchEntityForOrder(UUID merchId) {
        return merchItemRepository.findById(merchId)
            .filter(m -> m.getStatus() == MerchItemStatus.PUBLISHED)
            .orElseThrow(() -> new ResourceNotFoundException("Merch item", merchId.toString()));
    }

    @Transactional
    public void deductStock(UUID merchId, int quantity) {
        MerchItem item = merchItemRepository.findById(merchId)
            .orElseThrow(() -> new ResourceNotFoundException("Merch item", merchId.toString()));
        if (item.getStock() < quantity) {
            throw new ValidationException("Insufficient stock for: " + item.getName());
        }
        item.setStock(item.getStock() - quantity);
        merchItemRepository.save(item);
    }

    // ------------------------------------------------------------------ //
    //  PRIVATE HELPERS
    // ------------------------------------------------------------------ //

    private MerchItem findOwnItemOrThrow(UUID orgId, UUID merchId) {
        return merchItemRepository.findByIdAndOrgId(merchId, orgId)
            .orElseThrow(() -> new ResourceNotFoundException("Merch item", merchId.toString()));
    }

    private Category resolveCategory(String slug) {
        if (slug == null || slug.isBlank()) return null;
        return categoryRepository.findBySlug(slug.trim())
            .orElseThrow(() -> new ResourceNotFoundException("Category", slug));
    }

    private Map<UUID, Category> buildCategoryMap() {
        return categoryRepository.findAll().stream()
            .collect(Collectors.toMap(Category::getId, c -> c));
    }
}
