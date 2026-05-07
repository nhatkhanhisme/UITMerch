package com.uitmerch.backend.catalog.service;

import com.uitmerch.backend.auth.entity.User;
import com.uitmerch.backend.auth.repository.UserRepository;
import com.uitmerch.backend.catalog.dto.CreateMerchRequest;
import com.uitmerch.backend.catalog.dto.MerchItemResponse;
import com.uitmerch.backend.catalog.dto.MerchOrganizationInfo;
import com.uitmerch.backend.catalog.dto.SearchMerchRequest;
import com.uitmerch.backend.catalog.entity.MerchItem;
import com.uitmerch.backend.catalog.repository.MerchItemRepository;
import com.uitmerch.backend.common.exception.ResourceNotFoundException;
import com.uitmerch.backend.common.exception.ValidationException;
import com.uitmerch.backend.organizations.entity.Organization;
import com.uitmerch.backend.organizations.repository.OrganizationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Merch catalog service implementation.
 * BR07: Pre-order items may be created with zero stock, while ready-stock items require positive stock.
 */
@Service
@Transactional(readOnly = true)
public class MerchServiceImpl implements MerchService {

    private final MerchItemRepository merchItemRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;

    public MerchServiceImpl(
        MerchItemRepository merchItemRepository,
        OrganizationRepository organizationRepository,
        UserRepository userRepository
    ) {
        this.merchItemRepository = merchItemRepository;
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Page<MerchItemResponse> searchPublicMerch(SearchMerchRequest request) {
        int page = request.getPage() != null ? request.getPage() : 0;
        int pageSize = request.getPageSize() != null ? request.getPageSize() : 20;
        String sortBy = request.getSortBy() != null ? request.getSortBy() : "createdAt";
        Sort.Direction direction = request.getSortOrder() != null
            ? Sort.Direction.fromString(request.getSortOrder())
            : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(direction, sortBy));

        Specification<MerchItem> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(criteriaBuilder.isTrue(root.get("isActive")));

            if (request.getOrganizationId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("organization").get("id"), request.getOrganizationId()));
            }

            if (request.getIsPreorder() != null) {
                predicates.add(criteriaBuilder.equal(root.get("isPreorder"), request.getIsPreorder()));
            }

            if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
                String keyword = "%" + request.getKeyword().trim().toLowerCase(Locale.ROOT) + "%";
                Join<Object, Object> organizationJoin = root.join("organization");
                predicates.add(criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), keyword),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("meaningText")), keyword),
                    criteriaBuilder.like(criteriaBuilder.lower(organizationJoin.get("name")), keyword)
                ));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        return merchItemRepository.findAll(specification, pageable).map(this::toResponse);
    }

    @Override
    @Transactional
    public MerchItemResponse createMerch(CreateMerchRequest request) {
        User currentUser = getCurrentUser();
        Organization organization = organizationRepository.findTopByOwnerUserOrderByCreatedAtAsc(currentUser)
            .orElseThrow(() -> new ResourceNotFoundException("Organization not found for current organizer"));

        Integer stockQuantity = normalizeStockQuantity(request.getIsPreorder(), request.getStockQuantity());

        MerchItem merchItem = MerchItem.builder()
            .organization(organization)
            .name(request.getName().trim())
            .meaningText(request.getMeaningText() != null ? request.getMeaningText().trim() : null)
            .price(request.getPrice())
            .stockQuantity(stockQuantity)
            .isPreorder(request.getIsPreorder())
            .isActive(true)
            .imageUrl(request.getImageUrl() != null ? request.getImageUrl().trim() : null)
            .build();

        MerchItem savedMerchItem = merchItemRepository.save(merchItem);
        return toResponse(savedMerchItem);
    }

    private Integer normalizeStockQuantity(Boolean preorder, Integer stockQuantity) {
        if (Boolean.TRUE.equals(preorder)) {
            if (stockQuantity == null) {
                return 0;
            }
            if (stockQuantity < 0) {
                throw new ValidationException("Stock quantity cannot be negative");
            }
            return stockQuantity;
        }

        if (stockQuantity == null) {
            throw new ValidationException("Stock quantity is required for ready-stock items");
        }

        if (stockQuantity <= 0) {
            throw new ValidationException("Stock quantity must be greater than 0 for ready-stock items");
        }

        return stockQuantity;
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getName() == null) {
            throw new ValidationException("Authenticated user context is missing");
        }

        return userRepository.findByEmail(authentication.getName())
            .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }

    private MerchItemResponse toResponse(MerchItem merchItem) {
        return MerchItemResponse.builder()
            .id(merchItem.getId())
            .organization(MerchOrganizationInfo.builder()
                .id(merchItem.getOrganization() != null ? merchItem.getOrganization().getId() : null)
                .name(merchItem.getOrganization() != null ? merchItem.getOrganization().getName() : null)
                .logoUrl(merchItem.getOrganization() != null ? merchItem.getOrganization().getLogoUrl() : null)
                .build())
            .name(merchItem.getName())
            .meaningText(merchItem.getMeaningText())
            .price(merchItem.getPrice())
            .stockQuantity(merchItem.getStockQuantity())
            .isPreorder(merchItem.getIsPreorder())
            .isActive(merchItem.getIsActive())
            .imageUrl(merchItem.getImageUrl())
            .createdAt(merchItem.getCreatedAt())
            .updatedAt(merchItem.getUpdatedAt())
            .build();
    }
}