package com.uitmerch.backend.admin.dto;

import com.uitmerch.backend.auth.entity.User;
import com.uitmerch.backend.common.model.UserRole;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class UserSummaryResponse {

    private UUID id;
    private String email;
    private String fullName;
    private String phone;
    private UserRole role;
    private boolean isVerified;
    private boolean isActive;
    private LocalDateTime createdAt;

    public static UserSummaryResponse from(User user) {
        return UserSummaryResponse.builder()
            .id(user.getId())
            .email(user.getEmail())
            .fullName(user.getFullName())
            .phone(user.getPhone())
            .role(user.getRole())
            .isVerified(user.isVerified())
            .isActive(user.isActive())
            .createdAt(user.getCreatedAt())
            .build();
    }
}
