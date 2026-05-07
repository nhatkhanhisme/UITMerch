package com.uitmerch.backend.admin.service;

import com.uitmerch.backend.auth.dto.UpdateUserRoleRequest;
import com.uitmerch.backend.auth.dto.UserUpdateResponse;

import java.util.UUID;

/**
 * Admin service for managing users and system governance.
 * NFR02: Strict role checks at Controller/Route level using @PreAuthorize("hasRole('ADMIN')").
 */
public interface AdminService {

    /**
     * Update a user's role.
     * Only ADMIN users can promote/demote other users.
     * 
     * @param userId UUID of the user to update
     * @param request contains new role
     * @return updated user information with confirmation message
     */
    UserUpdateResponse updateUserRole(UUID userId, UpdateUserRoleRequest request);
}
