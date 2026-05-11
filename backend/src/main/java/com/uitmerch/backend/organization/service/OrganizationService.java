package com.uitmerch.backend.organization.service;

import com.uitmerch.backend.common.exception.ConflictException;
import com.uitmerch.backend.common.exception.ResourceNotFoundException;
import com.uitmerch.backend.common.model.OrganizationStatus;
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

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;

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

        return OrganizationResponse.from(organizationRepository.save(org));
    }

    public OrganizationResponse getOwnOrganization(UUID ownerId) {
        return OrganizationResponse.from(findByOwnerOrThrow(ownerId));
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

        return OrganizationResponse.from(organizationRepository.save(org));
    }

    public OrganizationResponse getOrganization(UUID orgId) {
        return OrganizationResponse.from(
            organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", orgId.toString()))
        );
    }

    public Page<OrganizationResponse> listActiveOrganizations(Pageable pageable) {
        return organizationRepository
            .findByStatus(OrganizationStatus.ACTIVE, pageable)
            .map(OrganizationResponse::from);
    }

    // Used by other modules (merch, order) to verify org exists and is ACTIVE
    public Organization getOwnOrganizationEntity(UUID ownerId) {
        return findByOwnerOrThrow(ownerId);
    }

    private Organization findByOwnerOrThrow(UUID ownerId) {
        return organizationRepository.findByOwnerId(ownerId)
            .orElseThrow(() -> new ResourceNotFoundException("Organization not found for this account."));
    }
}
