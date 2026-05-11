package com.uitmerch.backend.admin.service;

import com.uitmerch.backend.admin.dto.UserSummaryResponse;
import com.uitmerch.backend.auth.entity.User;
import com.uitmerch.backend.auth.repository.UserRepository;
import com.uitmerch.backend.common.exception.ResourceNotFoundException;
import com.uitmerch.backend.common.exception.ValidationException;
import com.uitmerch.backend.common.model.OrderStatus;
import com.uitmerch.backend.common.model.OrganizationStatus;
import com.uitmerch.backend.common.model.UserRole;
import com.uitmerch.backend.order.dto.OrderResponse;
import com.uitmerch.backend.order.service.OrderService;
import com.uitmerch.backend.organization.dto.OrganizationResponse;
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
public class AdminService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
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

    public Page<OrganizationResponse> listOrganizations(String statusFilter, Pageable pageable) {
        if (statusFilter != null) {
            OrganizationStatus status;
            try {
                status = OrganizationStatus.valueOf(statusFilter.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ValidationException("Invalid status filter: " + statusFilter);
            }
            return organizationRepository.findByStatus(status, pageable).map(OrganizationResponse::from);
        }
        return organizationRepository.findAll(pageable).map(OrganizationResponse::from);
    }

    @Transactional
    public OrganizationResponse updateOrganizationStatus(UUID orgId, OrganizationStatus newStatus) {
        Organization org = organizationRepository.findById(orgId)
            .orElseThrow(() -> new ResourceNotFoundException("Organization", orgId.toString()));
        org.setStatus(newStatus);
        return OrganizationResponse.from(organizationRepository.save(org));
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
}
