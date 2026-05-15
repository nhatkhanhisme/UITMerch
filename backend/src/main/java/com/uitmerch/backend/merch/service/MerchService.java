package com.uitmerch.backend.merch.service;

import com.uitmerch.backend.common.exception.ResourceNotFoundException;
import com.uitmerch.backend.common.exception.ValidationException;
import com.uitmerch.backend.common.model.MerchItemStatus;
import com.uitmerch.backend.common.model.OrganizationStatus;
import com.uitmerch.backend.merch.dto.CreateMerchRequest;
import com.uitmerch.backend.merch.dto.MerchResponse;
import com.uitmerch.backend.merch.dto.UpdateMerchRequest;
import com.uitmerch.backend.merch.entity.Category;
import com.uitmerch.backend.merch.entity.MerchImage;
import com.uitmerch.backend.merch.entity.MerchItem;
import com.uitmerch.backend.merch.repository.CategoryRepository;
import com.uitmerch.backend.merch.repository.MerchImageRepository;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MerchService {

    private final MerchItemRepository merchItemRepository;
    private final MerchImageRepository merchImageRepository;
    private final CategoryRepository categoryRepository;
    private final OrganizationService organizationService;
    private final OrderItemRepository orderItemRepository;

    // ------------------------------------------------------------------ //
    //  ORGANIZER
    // ------------------------------------------------------------------ //

    @Transactional
    public MerchResponse createMerch(UUID ownerId, UUID orgId, CreateMerchRequest request) {
        Organization org = organizationService.getOwnOrganizationEntity(ownerId, orgId);
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
            .categoryId(category != null ? category.getId() : null)
            .build();

        MerchItem saved = merchItemRepository.save(item);
        List<String> images = saveImages(saved.getId(), request.getImageUrls());
        return MerchResponse.from(saved, category, images);
    }

    @Transactional(readOnly = true)
    public Page<MerchResponse> getOwnMerch(UUID ownerId, UUID orgId, Pageable pageable) {
        Organization org = organizationService.getOwnOrganizationEntity(ownerId, orgId);
        Map<UUID, Category> categoryMap = buildCategoryMap();
        Page<MerchItem> page = merchItemRepository.findByOrgId(org.getId(), pageable);
        Map<UUID, List<String>> imageMap = buildImageMap(page.getContent());
        return page.map(item -> MerchResponse.from(item, categoryMap.get(item.getCategoryId()), imageMap.get(item.getId())));
    }

    @Transactional(readOnly = true)
    public MerchResponse getOwnMerchItem(UUID ownerId, UUID orgId, UUID merchId) {
        Organization org = organizationService.getOwnOrganizationEntity(ownerId, orgId);
        MerchItem item = findOwnItemOrThrow(org.getId(), merchId);
        Category category = item.getCategoryId() != null ? categoryRepository.findById(item.getCategoryId()).orElse(null) : null;
        List<String> images = loadImages(item.getId());
        return MerchResponse.from(item, category, images);
    }

    @Transactional
    public MerchResponse updateMerch(UUID ownerId, UUID orgId, UUID merchId, UpdateMerchRequest request) {
        Organization org = organizationService.getOwnOrganizationEntity(ownerId, orgId);
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

        MerchItem saved = merchItemRepository.save(item);

        List<String> images;
        if (request.getImageUrls() != null) {
            // null → keep existing; non-null (even empty) → replace all
            merchImageRepository.deleteByMerchId(saved.getId());
            images = saveImages(saved.getId(), request.getImageUrls());
        } else {
            images = loadImages(saved.getId());
        }

        return MerchResponse.from(saved, category, images);
    }

    @Transactional
    public void deleteMerch(UUID ownerId, UUID orgId, UUID merchId) {
        Organization org = organizationService.getOwnOrganizationEntity(ownerId, orgId);
        MerchItem item = findOwnItemOrThrow(org.getId(), merchId);
        item.setStatus(MerchItemStatus.ARCHIVED);
        merchItemRepository.save(item);
    }

    // ------------------------------------------------------------------ //
    //  PUBLIC
    // ------------------------------------------------------------------ //

    @Transactional(readOnly = true)
    public Page<MerchResponse> listPublished(String keyword, String categorySlug, Pageable pageable) {
        Map<UUID, Category> categoryMap = buildCategoryMap();
        Page<MerchItem> page;

        if (categorySlug != null && !categorySlug.isBlank()) {
            Category category = categoryRepository.findBySlug(categorySlug.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Category", categorySlug));
            UUID catId = category.getId();

            page = (keyword != null && !keyword.isBlank())
                ? merchItemRepository.findByStatusAndCategoryIdAndNameContainingIgnoreCase(MerchItemStatus.PUBLISHED, catId, keyword.trim(), pageable)
                : merchItemRepository.findByStatusAndCategoryId(MerchItemStatus.PUBLISHED, catId, pageable);
        } else if (keyword != null && !keyword.isBlank()) {
            page = merchItemRepository.findByStatusAndNameContainingIgnoreCase(MerchItemStatus.PUBLISHED, keyword.trim(), pageable);
        } else {
            page = merchItemRepository.findByStatus(MerchItemStatus.PUBLISHED, pageable);
        }

        Map<UUID, List<String>> imageMap = buildImageMap(page.getContent());
        return page.map(item -> MerchResponse.from(item, categoryMap.get(item.getCategoryId()), imageMap.get(item.getId())));
    }

    @Transactional(readOnly = true)
    public MerchResponse getPublishedMerch(UUID merchId) {
        MerchItem item = merchItemRepository.findById(merchId)
            .filter(m -> m.getStatus() == MerchItemStatus.PUBLISHED)
            .orElseThrow(() -> new ResourceNotFoundException("Merch item", merchId.toString()));
        Category category = item.getCategoryId() != null ? categoryRepository.findById(item.getCategoryId()).orElse(null) : null;
        List<String> images = loadImages(item.getId());
        return MerchResponse.from(item, category, images);
    }

    @Transactional(readOnly = true)
    public List<MerchResponse> getPopularMerch() {
        List<MerchItem> published = merchItemRepository.findAllByStatus(MerchItemStatus.PUBLISHED);
        if (published.isEmpty()) return List.of();

        List<UUID> ids = published.stream().map(MerchItem::getId).toList();
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        Map<UUID, Long> allTimeOrders = toOrderCountMap(orderItemRepository.sumQuantityByMerchIds(ids));
        Map<UUID, Long> recentOrders  = toOrderCountMap(orderItemRepository.sumQuantityByMerchIdsSince(ids, thirtyDaysAgo));

        Map<UUID, Category> categoryMap = buildCategoryMap();
        Map<UUID, List<String>> imageMap = buildImageMap(published);
        LocalDateTime now = LocalDateTime.now();

        return published.stream()
            .sorted(Comparator.comparingDouble(item -> -popularityScore(item, allTimeOrders, recentOrders, now)))
            .limit(10)
            .map(item -> MerchResponse.from(item, categoryMap.get(item.getCategoryId()), imageMap.get(item.getId())))
            .toList();
    }

    @Transactional(readOnly = true)
    public Page<MerchResponse> listByOrganization(UUID orgId, Pageable pageable) {
        Map<UUID, Category> categoryMap = buildCategoryMap();
        Page<MerchItem> page = merchItemRepository.findByOrgIdAndStatus(orgId, MerchItemStatus.PUBLISHED, pageable);
        Map<UUID, List<String>> imageMap = buildImageMap(page.getContent());
        return page.map(item -> MerchResponse.from(item, categoryMap.get(item.getCategoryId()), imageMap.get(item.getId())));
    }

    // ------------------------------------------------------------------ //
    //  PACKAGE-INTERNAL
    // ------------------------------------------------------------------ //

    @Transactional(readOnly = true)
    public MerchItem getMerchEntityForOrder(UUID merchId) {
        return merchItemRepository.findById(merchId)
            .filter(m -> m.getStatus() == MerchItemStatus.PUBLISHED)
            .orElseThrow(() -> new ResourceNotFoundException("Merch item", merchId.toString()));
    }

    // ------------------------------------------------------------------ //
    //  PRIVATE HELPERS
    // ------------------------------------------------------------------ //

    private List<String> saveImages(UUID merchId, List<String> urls) {
        if (urls == null || urls.isEmpty()) return List.of();
        List<MerchImage> toSave = new ArrayList<>();
        for (int i = 0; i < urls.size(); i++) {
            toSave.add(MerchImage.builder().merchId(merchId).url(urls.get(i)).position(i).build());
        }
        merchImageRepository.saveAll(toSave);
        return urls;
    }

    private List<String> loadImages(UUID merchId) {
        return merchImageRepository.findByMerchIdOrderByPosition(merchId)
            .stream().map(MerchImage::getUrl).toList();
    }

    private Map<UUID, List<String>> buildImageMap(List<MerchItem> items) {
        if (items.isEmpty()) return Map.of();
        List<UUID> ids = items.stream().map(MerchItem::getId).toList();
        return merchImageRepository.findByMerchIdInOrderByPosition(ids)
            .stream()
            .collect(Collectors.groupingBy(
                MerchImage::getMerchId,
                Collectors.mapping(MerchImage::getUrl, Collectors.toList())
            ));
    }

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
}
