package com.uitmerch.backend.organization.service;

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
        Organization org = Organization.builder()
            .ownerId(ownerId)
            .name(request.getName())
            .description(request.getDescription())
            .build();

        return OrganizationResponse.from(organizationRepository.save(org), 0L);
    }

    @Transactional(readOnly = true)
    public Page<OrganizationResponse> getOwnOrganizations(UUID ownerId, Pageable pageable) {
        Page<Organization> page = organizationRepository.findByOwnerId(ownerId, pageable);
        Map<UUID, Long> counts = batchCountPublishedMerch(page.getContent().stream().map(Organization::getId).toList());
        return page.map(org -> OrganizationResponse.from(org, counts.getOrDefault(org.getId(), 0L)));
    }

    @Transactional
    public OrganizationResponse updateOrganization(UUID ownerId, UUID orgId, UpdateOrganizationRequest request) {
        Organization org = organizationRepository.findByIdAndOwnerId(orgId, ownerId)
            .orElseThrow(() -> new ResourceNotFoundException("Organization not found for this account."));

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

    @Transactional(readOnly = true)
    public OrganizationResponse getOrganization(UUID orgId) {
        Organization org = organizationRepository.findById(orgId)
            .orElseThrow(() -> new ResourceNotFoundException("Organization", orgId.toString()));
        return OrganizationResponse.from(org, countPublishedMerch(org.getId()));
    }

    @Transactional(readOnly = true)
    public Page<OrganizationResponse> listActiveOrganizations(Pageable pageable) {
        Page<Organization> page = organizationRepository.findByStatus(OrganizationStatus.ACTIVE, pageable);
        Map<UUID, Long> counts = batchCountPublishedMerch(page.getContent().stream().map(Organization::getId).toList());
        return page.map(org -> OrganizationResponse.from(org, counts.getOrDefault(org.getId(), 0L)));
    }

    // Used by other modules (merch, event, order) to verify ownership and org exists
    @Transactional(readOnly = true)
    public Organization getOwnOrganizationEntity(UUID ownerId, UUID orgId) {
        return organizationRepository.findByIdAndOwnerId(orgId, ownerId)
            .orElseThrow(() -> new ResourceNotFoundException("Organization not found for this account."));
    }

    // Used when we need org data without ownership check (e.g. notifying org owner on customer cancel)
    @Transactional(readOnly = true)
    public Organization getOrganizationEntityById(UUID orgId) {
        return organizationRepository.findById(orgId)
            .orElseThrow(() -> new ResourceNotFoundException("Organization", orgId.toString()));
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
