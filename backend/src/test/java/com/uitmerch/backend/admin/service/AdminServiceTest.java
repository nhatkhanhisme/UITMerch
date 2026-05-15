package com.uitmerch.backend.admin.service;

import com.uitmerch.backend.admin.dto.UserSummaryResponse;
import com.uitmerch.backend.auth.entity.User;
import com.uitmerch.backend.auth.repository.UserRepository;
import com.uitmerch.backend.common.exception.ResourceNotFoundException;
import com.uitmerch.backend.common.exception.ValidationException;
import com.uitmerch.backend.common.model.MerchItemStatus;
import com.uitmerch.backend.common.model.OrganizationStatus;
import com.uitmerch.backend.common.model.UserRole;
import com.uitmerch.backend.merch.repository.MerchItemRepository;
import com.uitmerch.backend.order.service.OrderService;
import com.uitmerch.backend.organization.dto.OrganizationResponse;
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
class AdminServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private OrganizationRepository organizationRepository;
    @Mock private MerchItemRepository merchItemRepository;
    @Mock private OrderService orderService;

    @InjectMocks private AdminService adminService;

    private final UUID userId = UUID.randomUUID();
    private final UUID orgId  = UUID.randomUUID();

    private User user(UserRole role) {
        return User.builder().id(userId).email("u@uit.edu.vn")
            .fullName("U").passwordHash("h").role(role)
            .isVerified(true).isActive(true).build();
    }

    private Organization org(OrganizationStatus status) {
        return Organization.builder().id(orgId).ownerId(userId)
            .name("Org").status(status).build();
    }

    // ── listUsers ────────────────────────────────────────────────────────────

    @Test
    void listUsers_noFilter_returnsAll() {
        when(userRepository.findAll(any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(user(UserRole.CUSTOMER))));

        var page = adminService.listUsers(null, Pageable.unpaged());

        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    void listUsers_withRoleFilter_filtersResults() {
        when(userRepository.findByRole(eq(UserRole.ORGANIZER), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(user(UserRole.ORGANIZER))));

        var page = adminService.listUsers("ORGANIZER", Pageable.unpaged());

        assertThat(page.getContent().get(0).getRole()).isEqualTo(UserRole.ORGANIZER);
    }

    @Test
    void listUsers_invalidRole_throwsValidation() {
        assertThatThrownBy(() -> adminService.listUsers("SUPERUSER", Pageable.unpaged()))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Invalid role filter");
    }

    // ── updateUserRole ───────────────────────────────────────────────────────

    @Test
    void updateUserRole_success_changesRole() {
        User u = user(UserRole.CUSTOMER);
        when(userRepository.findById(userId)).thenReturn(Optional.of(u));
        when(userRepository.save(u)).thenReturn(u);

        UserSummaryResponse response = adminService.updateUserRole(userId, UserRole.ORGANIZER);

        assertThat(response.getRole()).isEqualTo(UserRole.ORGANIZER);
        verify(userRepository).save(u);
    }

    @Test
    void updateUserRole_userNotFound_throwsResourceNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.updateUserRole(userId, UserRole.ADMIN))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── setUserActive ────────────────────────────────────────────────────────

    @Test
    void setUserActive_deactivate_setsActiveToFalse() {
        User u = user(UserRole.CUSTOMER);
        when(userRepository.findById(userId)).thenReturn(Optional.of(u));
        when(userRepository.save(u)).thenReturn(u);

        adminService.setUserActive(userId, false);

        assertThat(u.isActive()).isFalse();
        verify(userRepository).save(u);
    }

    @Test
    void setUserActive_activate_setsActiveToTrue() {
        User u = user(UserRole.CUSTOMER);
        u.setActive(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(u));
        when(userRepository.save(u)).thenReturn(u);

        adminService.setUserActive(userId, true);

        assertThat(u.isActive()).isTrue();
    }

    @Test
    void setUserActive_userNotFound_throwsResourceNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.setUserActive(userId, false))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── listOrganizations ────────────────────────────────────────────────────

    @Test
    void listOrganizations_noFilter_returnsAll() {
        when(organizationRepository.findAll(any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(org(OrganizationStatus.ACTIVE))));
        when(merchItemRepository.countByOrgIdsAndStatus(any(), eq(MerchItemStatus.PUBLISHED)))
            .thenReturn(List.of());

        var page = adminService.listOrganizations(null, Pageable.unpaged());

        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    void listOrganizations_invalidStatus_throwsValidation() {
        assertThatThrownBy(() -> adminService.listOrganizations("UNKNOWN", Pageable.unpaged()))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Invalid status filter");
    }

    // ── updateOrganizationStatus ─────────────────────────────────────────────

    @Test
    void updateOrganizationStatus_toSuspended_archivesPublishedMerch() {
        Organization o = org(OrganizationStatus.ACTIVE);
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(o));
        when(organizationRepository.save(o)).thenReturn(o);
        when(merchItemRepository.archivePublishedByOrgId(orgId)).thenReturn(3);
        when(merchItemRepository.countByOrgIdAndStatus(orgId, MerchItemStatus.PUBLISHED)).thenReturn(0L);

        OrganizationResponse response = adminService.updateOrganizationStatus(orgId, OrganizationStatus.SUSPENDED);

        assertThat(response).isNotNull();
        verify(merchItemRepository).archivePublishedByOrgId(orgId);
    }

    @Test
    void updateOrganizationStatus_toActive_doesNotArchiveMerch() {
        Organization o = org(OrganizationStatus.PENDING);
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(o));
        when(organizationRepository.save(o)).thenReturn(o);
        when(merchItemRepository.countByOrgIdAndStatus(orgId, MerchItemStatus.PUBLISHED)).thenReturn(2L);

        adminService.updateOrganizationStatus(orgId, OrganizationStatus.ACTIVE);

        verify(merchItemRepository, org.mockito.Mockito.never()).archivePublishedByOrgId(any());
    }

    @Test
    void updateOrganizationStatus_orgNotFound_throwsResourceNotFound() {
        when(organizationRepository.findById(orgId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.updateOrganizationStatus(orgId, OrganizationStatus.SUSPENDED))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
