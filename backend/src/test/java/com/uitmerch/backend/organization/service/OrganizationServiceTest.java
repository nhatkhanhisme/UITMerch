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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {

    @Mock private OrganizationRepository organizationRepository;
    @Mock private MerchItemRepository merchItemRepository;

    @InjectMocks private OrganizationService organizationService;

    private final UUID ownerId = UUID.randomUUID();
    private final UUID orgId   = UUID.randomUUID();

    private Organization activeOrg() {
        return Organization.builder().id(orgId).ownerId(ownerId)
            .name("Test Org").status(OrganizationStatus.ACTIVE).build();
    }

    // ── createOrganization ───────────────────────────────────────────────────

    @Test
    void createOrganization_success_savedWithOwnerAndPendingStatus() {
        Organization saved = Organization.builder().id(orgId).ownerId(ownerId)
            .name("New Org").status(OrganizationStatus.PENDING).build();
        when(organizationRepository.save(any())).thenReturn(saved);
        when(merchItemRepository.countByOrgIdAndStatus(orgId, MerchItemStatus.PUBLISHED)).thenReturn(0L);

        CreateOrganizationRequest req = new CreateOrganizationRequest();
        req.setName("New Org");

        OrganizationResponse response = organizationService.createOrganization(ownerId, req);

        assertThat(response.getName()).isEqualTo("New Org");
        verify(organizationRepository).save(any());
    }

    // ── updateOrganization ───────────────────────────────────────────────────

    @Test
    void updateOrganization_partialUpdate_onlyModifiesProvidedFields() {
        Organization org = activeOrg();
        when(organizationRepository.findByIdAndOwnerId(orgId, ownerId)).thenReturn(Optional.of(org));
        when(organizationRepository.save(org)).thenReturn(org);
        when(merchItemRepository.countByOrgIdAndStatus(orgId, MerchItemStatus.PUBLISHED)).thenReturn(0L);

        UpdateOrganizationRequest req = new UpdateOrganizationRequest();
        req.setName("Updated Name");
        // description not set — should remain unchanged

        OrganizationResponse response = organizationService.updateOrganization(ownerId, orgId, req);

        assertThat(org.getName()).isEqualTo("Updated Name");
        assertThat(response).isNotNull();
    }

    @Test
    void updateOrganization_wrongOwner_throwsResourceNotFound() {
        when(organizationRepository.findByIdAndOwnerId(orgId, ownerId)).thenReturn(Optional.empty());

        UpdateOrganizationRequest req = new UpdateOrganizationRequest();
        req.setName("Hack");

        assertThatThrownBy(() -> organizationService.updateOrganization(ownerId, orgId, req))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getOrganization ──────────────────────────────────────────────────────

    @Test
    void getOrganization_exists_returnsResponse() {
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(activeOrg()));
        when(merchItemRepository.countByOrgIdAndStatus(orgId, MerchItemStatus.PUBLISHED)).thenReturn(5L);

        OrganizationResponse response = organizationService.getOrganization(orgId);

        assertThat(response.getId()).isEqualTo(orgId);
        assertThat(response.getMerchCount()).isEqualTo(5L);
    }

    @Test
    void getOrganization_notFound_throwsResourceNotFound() {
        when(organizationRepository.findById(orgId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> organizationService.getOrganization(orgId))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── listActiveOrganizations ──────────────────────────────────────────────

    @Test
    void listActiveOrganizations_returnsOnlyActiveOrgs() {
        when(organizationRepository.findByStatus(eq(OrganizationStatus.ACTIVE), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(activeOrg())));
        when(merchItemRepository.countByOrgIdsAndStatus(any(), eq(MerchItemStatus.PUBLISHED)))
            .thenReturn(List.of());

        var page = organizationService.listActiveOrganizations(Pageable.unpaged());

        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    // ── getOwnOrganizationEntity ─────────────────────────────────────────────

    @Test
    void getOwnOrganizationEntity_correctOwner_returnsOrg() {
        when(organizationRepository.findByIdAndOwnerId(orgId, ownerId)).thenReturn(Optional.of(activeOrg()));

        Organization org = organizationService.getOwnOrganizationEntity(ownerId, orgId);

        assertThat(org.getId()).isEqualTo(orgId);
    }

    @Test
    void getOwnOrganizationEntity_wrongOwner_throwsResourceNotFound() {
        when(organizationRepository.findByIdAndOwnerId(orgId, ownerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> organizationService.getOwnOrganizationEntity(ownerId, orgId))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
