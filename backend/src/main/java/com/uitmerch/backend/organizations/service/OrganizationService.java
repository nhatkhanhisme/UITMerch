package com.uitmerch.backend.organizations.service;

import com.uitmerch.backend.organizations.dto.OrganizationResponse;
import com.uitmerch.backend.organizations.dto.OrganizationUpdateRequest;

import java.util.UUID;

/**
 * Service contract for organization management.
 */
public interface OrganizationService {

    OrganizationResponse getMyOrganization();

    OrganizationResponse updateOrganization(UUID organizationId, OrganizationUpdateRequest request);
}