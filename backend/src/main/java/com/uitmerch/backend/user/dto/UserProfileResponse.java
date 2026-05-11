package com.uitmerch.backend.user.dto;

import com.uitmerch.backend.auth.entity.User;
import com.uitmerch.backend.common.model.UserRole;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class UserProfileResponse {

    private UUID id;
    private String email;
    private String fullName;
    private String phone;
    private String address;
    private String avatarUrl;
    private UserRole role;
    private LocalDateTime createdAt;

    public static UserProfileResponse from(User user) {
        return UserProfileResponse.builder()
            .id(user.getId())
            .email(user.getEmail())
            .fullName(user.getFullName())
            .phone(user.getPhone())
            .address(user.getAddress())
            .avatarUrl(user.getAvatarUrl())
            .role(user.getRole())
            .createdAt(user.getCreatedAt())
            .build();
    }
}
