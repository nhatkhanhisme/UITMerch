package com.uitmerch.backend.admin.service;

import com.uitmerch.backend.auth.dto.UpdateUserRoleRequest;
import com.uitmerch.backend.auth.dto.UserUpdateResponse;
import com.uitmerch.backend.auth.entity.User;
import com.uitmerch.backend.auth.repository.UserRepository;
import com.uitmerch.backend.common.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Implementation of AdminService for managing users and system governance.
 * NFR02: Strict role checks at Controller/Route level using @PreAuthorize("hasRole('ADMIN')").
 */
@Service
public class AdminServiceImpl implements AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminServiceImpl.class);

    private final UserRepository userRepository;

    public AdminServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public UserUpdateResponse updateUserRole(UUID userId, UpdateUserRoleRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        String oldRole = user.getRole().name();
        user.setRole(request.getRole());
        User updatedUser = userRepository.save(user);

        log.info("User {} role updated from {} to {}", userId, oldRole, request.getRole().name());

        return UserUpdateResponse.builder()
            .userId(updatedUser.getId().toString())
            .email(updatedUser.getEmail())
            .fullName(updatedUser.getFullName())
            .role(updatedUser.getRole())
            .message(String.format("User role updated from %s to %s", oldRole, request.getRole().name()))
            .build();
    }
}
