package com.uitmerch.backend.user.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {

    private String fullName;
    private String phone;
    private String address;
    private String avatarUrl;
}
