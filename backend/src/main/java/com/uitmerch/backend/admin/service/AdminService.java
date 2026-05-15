package com.uitmerch.backend.admin.service;

import com.uitmerch.backend.admin.dto.UserSummaryResponse;
import com.uitmerch.backend.auth.entity.User;
import com.uitmerch.backend.auth.repository.UserRepository;
import com.uitmerch.backend.common.exception.ResourceNotFoundException;
import com.uitmerch.backend.common.exception.ValidationException;
import com.uitmerch.backend.common.model.MerchItemStatus;
import com.uitmerch.backend.common.model.OrderStatus;
import com.uitmerch.backend.common.model.OrganizationStatus;
import com.uitmerch.backend.common.model.UserRole;
import com.uitmerch.backend.merch.repository.MerchItemRepository;
import com.uitmerch.backend.order.dto.OrderResponse;
import com.uitmerch.backend.order.service.OrderService;
import com.uitmerch.backend.organization.dto.OrganizationResponse;
import com.uitmerch.backend.organization.entity.Organization;
import com.uitmerch.backend.organization.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final MerchItemRepository merchItemRepository;
    private final OrderService orderService;

    public Page<UserSummaryResponse> listUsers(String roleFilter, Pageable pageable) {
        if (roleFilter != null) {
            UserRole role;
            try {
                role = UserRole.valueOf(roleFilter.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ValidationException("Invalid role filter: " + roleFilter);
            }
            return userRepository.findByRole(role, pageable).map(UserSummaryResponse::from);
        }
        return userRepository.findAll(pageable).map(UserSummaryResponse::from);
    }

    @Transactional
    public UserSummaryResponse updateUserRole(UUID userId, UserRole newRole) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));
        user.setRole(newRole);
        return UserSummaryResponse.from(userRepository.save(user));
    }

    @Transactional
    public UserSummaryResponse setUserActive(UUID userId, boolean active) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));
        user.setActive(active);
        log.info("User {} {} by admin", userId, active ? "activated" : "deactivated");
        return UserSummaryResponse.from(userRepository.save(user));
    }

    public Page<OrganizationResponse> listOrganizations(String statusFilter, Pageable pageable) {
        Page<Organization> page;
        if (statusFilter != null) {
            OrganizationStatus status;
            try {
                status = OrganizationStatus.valueOf(statusFilter.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ValidationException("Invalid status filter: " + statusFilter);
            }
            page = organizationRepository.findByStatus(status, pageable);
        } else {
            page = organizationRepository.findAll(pageable);
        }
        Map<UUID, Long> counts = batchCountPublishedMerch(page.getContent().stream().map(Organization::getId).toList());
        return page.map(org -> OrganizationResponse.from(org, counts.getOrDefault(org.getId(), 0L)));
    }

    @Transactional
    public OrganizationResponse updateOrganizationStatus(UUID orgId, OrganizationStatus newStatus) {
        Organization org = organizationRepository.findById(orgId)
            .orElseThrow(() -> new ResourceNotFoundException("Organization", orgId.toString()));
        org.setStatus(newStatus);
        Organization saved = organizationRepository.save(org);

        // When an org loses ACTIVE status, its published merch must be taken off sale
        if (newStatus != OrganizationStatus.ACTIVE) {
            int archived = merchItemRepository.archivePublishedByOrgId(saved.getId());
            if (archived > 0) {
                log.info("Archived {} published merch items for suspended org {}", archived, saved.getId());
            }
        }

        long count = merchItemRepository.countByOrgIdAndStatus(saved.getId(), MerchItemStatus.PUBLISHED);
        return OrganizationResponse.from(saved, count);
    }

    public Page<OrderResponse> listAllOrders(String statusFilter, Pageable pageable) {
        if (statusFilter != null) {
            OrderStatus status;
            try {
                status = OrderStatus.valueOf(statusFilter.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ValidationException("Invalid status filter: " + statusFilter);
            }
            return orderService.getAllOrders(status, pageable);
        }
        return orderService.getAllOrders(null, pageable);
    }

    private Map<UUID, Long> batchCountPublishedMerch(List<UUID> orgIds) {
        if (orgIds.isEmpty()) return Map.of();
        return merchItemRepository.countByOrgIdsAndStatus(orgIds, MerchItemStatus.PUBLISHED)
            .stream()
            .collect(Collectors.toMap(row -> (UUID) row[0], row -> (Long) row[1]));
    }
}
