package com.uitmerch.backend.organization.service;

import com.uitmerch.backend.common.exception.ConflictException;
import com.uitmerch.backend.common.exception.ResourceNotFoundException;
import com.uitmerch.backend.common.model.MerchItemStatus;
import com.uitmerch.backend.common.model.OrganizationStatus;
import com.uitmerch.backend.merch.repository.MerchItemRepository;
import com.uitmerch.backend.organization.dto.CreateOrganizationRequest;
import com.uitmerch.backend.organization.dto.OrganizationResponse;
import com.uitmerch.backend.organization.dto.UpdateOrganizationRequest;
import com.uitmerch.backend.organization.entity.Organization;
import com.uitmerch.backend.organization.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final MerchItemRepository merchItemRepository;

    @Transactional
    public OrganizationResponse createOrganization(UUID ownerId, CreateOrganizationRequest request) {
        if (organizationRepository.existsByOwnerId(ownerId)) {
            throw new ConflictException("You already have an organization.");
        }

        Organization org = Organization.builder()
            .ownerId(ownerId)
            .name(request.getName())
            .description(request.getDescription())
            .build();

        return OrganizationResponse.from(organizationRepository.save(org), 0L);
    }

    public OrganizationResponse getOwnOrganization(UUID ownerId) {
        Organization org = findByOwnerOrThrow(ownerId);
        return OrganizationResponse.from(org, countPublishedMerch(org.getId()));
    }

    @Transactional
    public OrganizationResponse updateOrganization(UUID ownerId, UpdateOrganizationRequest request) {
        Organization org = findByOwnerOrThrow(ownerId);

        if (request.getName() != null && !request.getName().isBlank()) {
            org.setName(request.getName());
        }
        if (request.getDescription() != null) {
            org.setDescription(request.getDescription());
        }
        if (request.getLogoUrl() != null) {
            org.setLogoUrl(request.getLogoUrl());
        }
        if (request.getCoverUrl() != null) {
            org.setCoverUrl(request.getCoverUrl());
        }

        Organization saved = organizationRepository.save(org);
        return OrganizationResponse.from(saved, countPublishedMerch(saved.getId()));
    }

    public OrganizationResponse getOrganization(UUID orgId) {
        Organization org = organizationRepository.findById(orgId)
            .orElseThrow(() -> new ResourceNotFoundException("Organization", orgId.toString()));
        return OrganizationResponse.from(org, countPublishedMerch(org.getId()));
    }

    public Page<OrganizationResponse> listActiveOrganizations(Pageable pageable) {
        Page<Organization> page = organizationRepository.findByStatus(OrganizationStatus.ACTIVE, pageable);
        Map<UUID, Long> counts = batchCountPublishedMerch(page.getContent().stream().map(Organization::getId).toList());
        return page.map(org -> OrganizationResponse.from(org, counts.getOrDefault(org.getId(), 0L)));
    }

    // Used by other modules (merch, order) to verify org exists and is ACTIVE
    public Organization getOwnOrganizationEntity(UUID ownerId) {
        return findByOwnerOrThrow(ownerId);
    }

    private Organization findByOwnerOrThrow(UUID ownerId) {
        return organizationRepository.findByOwnerId(ownerId)
            .orElseThrow(() -> new ResourceNotFoundException("Organization not found for this account."));
    }

    private long countPublishedMerch(UUID orgId) {
        return merchItemRepository.countByOrgIdAndStatus(orgId, MerchItemStatus.PUBLISHED);
    }

    private Map<UUID, Long> batchCountPublishedMerch(List<UUID> orgIds) {
        if (orgIds.isEmpty()) return Map.of();
        return merchItemRepository.countByOrgIdsAndStatus(orgIds, MerchItemStatus.PUBLISHED)
            .stream()
            .collect(Collectors.toMap(row -> (UUID) row[0], row -> (Long) row[1]));
    }
}
