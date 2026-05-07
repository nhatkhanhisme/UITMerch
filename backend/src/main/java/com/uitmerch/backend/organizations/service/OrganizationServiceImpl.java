package com.uitmerch.backend.organizations.service;

import com.uitmerch.backend.auth.entity.User;
import com.uitmerch.backend.auth.repository.UserRepository;
import com.uitmerch.backend.common.exception.ResourceNotFoundException;
import com.uitmerch.backend.common.exception.ValidationException;
import com.uitmerch.backend.organizations.dto.OrganizationResponse;
import com.uitmerch.backend.organizations.dto.OrganizationUpdateRequest;
import com.uitmerch.backend.organizations.entity.Organization;
import com.uitmerch.backend.organizations.repository.OrganizationRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Organization management service.
 * Enforces that an organizer can only update their own organization.
 */
@Service
@Transactional(readOnly = true)
public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;

    public OrganizationServiceImpl(
        OrganizationRepository organizationRepository,
        UserRepository userRepository
    ) {
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
    }

    @Override
    public OrganizationResponse getMyOrganization() {
        User currentUser = getCurrentUser();

        Organization organization = organizationRepository.findTopByOwnerUserOrderByCreatedAtAsc(currentUser)
            .orElseThrow(() -> new ResourceNotFoundException("Organization not found for current organizer"));

        return toResponse(organization);
    }

    @Override
    @Transactional
    public OrganizationResponse updateOrganization(UUID organizationId, OrganizationUpdateRequest request) {
        User currentUser = getCurrentUser();

        Organization organization = organizationRepository.findByIdAndOwnerUser(organizationId, currentUser)
            .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        boolean hasAnyUpdate = false;

        if (request.getName() != null) {
            String name = request.getName().trim();
            if (name.isEmpty()) {
                throw new ValidationException("Organization name must not be blank");
            }
            organization.setName(name);
            hasAnyUpdate = true;
        }

        if (request.getDescription() != null) {
            String description = request.getDescription().trim();
            if (description.isEmpty()) {
                throw new ValidationException("Organization description must not be blank");
            }
            organization.setDescription(description);
            hasAnyUpdate = true;
        }

        if (request.getLogoUrl() != null) {
            String logoUrl = request.getLogoUrl().trim();
            if (logoUrl.isEmpty()) {
                throw new ValidationException("Organization logo URL must not be blank");
            }
            organization.setLogoUrl(logoUrl);
            hasAnyUpdate = true;
        }

        if (!hasAnyUpdate) {
            throw new ValidationException("At least one organization field must be provided for update");
        }

        Organization savedOrganization = organizationRepository.save(organization);
        return toResponse(savedOrganization);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getName() == null) {
            throw new ValidationException("Authenticated user context is missing");
        }

        return userRepository.findByEmail(authentication.getName())
            .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }

    private OrganizationResponse toResponse(Organization organization) {
        return OrganizationResponse.builder()
            .id(organization.getId())
            .ownerUserId(organization.getOwnerUser() != null ? organization.getOwnerUser().getId() : null)
            .name(organization.getName())
            .description(organization.getDescription())
            .logoUrl(organization.getLogoUrl())
            .status(organization.getStatus())
            .createdAt(organization.getCreatedAt())
            .updatedAt(organization.getUpdatedAt())
            .build();
    }
}