package com.uitmerch.backend.merch.service;

import com.uitmerch.backend.common.exception.ResourceNotFoundException;
import com.uitmerch.backend.common.exception.ValidationException;
import com.uitmerch.backend.common.model.MerchItemStatus;
import com.uitmerch.backend.common.model.OrganizationStatus;
import com.uitmerch.backend.merch.dto.CreateMerchRequest;
import com.uitmerch.backend.merch.dto.MerchResponse;
import com.uitmerch.backend.merch.dto.UpdateMerchRequest;
import com.uitmerch.backend.merch.entity.MerchItem;
import com.uitmerch.backend.merch.repository.MerchItemRepository;
import com.uitmerch.backend.organization.entity.Organization;
import com.uitmerch.backend.organization.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MerchService {

    private final MerchItemRepository merchItemRepository;
    private final OrganizationService organizationService;

    // ------------------------------------------------------------------ //
    //  ORGANIZER
    // ------------------------------------------------------------------ //

    @Transactional
    public MerchResponse createMerch(UUID ownerId, CreateMerchRequest request) {
        Organization org = organizationService.getOwnOrganizationEntity(ownerId);
        if (org.getStatus() != OrganizationStatus.ACTIVE) {
            throw new ValidationException("Your organization must be ACTIVE to create merch. Current status: " + org.getStatus());
        }

        MerchItem item = MerchItem.builder()
            .orgId(org.getId())
            .name(request.getName())
            .description(request.getDescription())
            .price(request.getPrice())
            .stock(request.getStock())
            .imageUrl(request.getImageUrl())
            .build();

        return MerchResponse.from(merchItemRepository.save(item));
    }

    public Page<MerchResponse> getOwnMerch(UUID ownerId, Pageable pageable) {
        Organization org = organizationService.getOwnOrganizationEntity(ownerId);
        return merchItemRepository.findByOrgId(org.getId(), pageable).map(MerchResponse::from);
    }

    public MerchResponse getOwnMerchItem(UUID ownerId, UUID merchId) {
        Organization org = organizationService.getOwnOrganizationEntity(ownerId);
        return MerchResponse.from(findOwnItemOrThrow(org.getId(), merchId));
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

        return MerchResponse.from(merchItemRepository.save(item));
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

    public Page<MerchResponse> listPublished(String keyword, Pageable pageable) {
        if (keyword != null && !keyword.isBlank()) {
            return merchItemRepository
                .findByStatusAndNameContainingIgnoreCase(MerchItemStatus.PUBLISHED, keyword.trim(), pageable)
                .map(MerchResponse::from);
        }
        return merchItemRepository.findByStatus(MerchItemStatus.PUBLISHED, pageable).map(MerchResponse::from);
    }

    public MerchResponse getPublishedMerch(UUID merchId) {
        MerchItem item = merchItemRepository.findById(merchId)
            .filter(m -> m.getStatus() == MerchItemStatus.PUBLISHED)
            .orElseThrow(() -> new ResourceNotFoundException("Merch item", merchId.toString()));
        return MerchResponse.from(item);
    }

    public List<MerchResponse> getPopularMerch() {
        return merchItemRepository
            .findTop10ByStatusOrderByCreatedAtDesc(MerchItemStatus.PUBLISHED)
            .stream().map(MerchResponse::from).toList();
    }

    public Page<MerchResponse> listByOrganization(UUID orgId, Pageable pageable) {
        return merchItemRepository
            .findByOrgIdAndStatus(orgId, MerchItemStatus.PUBLISHED, pageable)
            .map(MerchResponse::from);
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

    private MerchItem findOwnItemOrThrow(UUID orgId, UUID merchId) {
        return merchItemRepository.findByIdAndOrgId(merchId, orgId)
            .orElseThrow(() -> new ResourceNotFoundException("Merch item", merchId.toString()));
    }
}
