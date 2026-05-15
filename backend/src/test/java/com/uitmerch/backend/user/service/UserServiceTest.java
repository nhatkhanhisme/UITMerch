package com.uitmerch.backend.user.service;

import com.uitmerch.backend.auth.entity.User;
import com.uitmerch.backend.auth.repository.UserRepository;
import com.uitmerch.backend.common.exception.ResourceNotFoundException;
import com.uitmerch.backend.common.model.UserRole;
import com.uitmerch.backend.user.dto.UpdateProfileRequest;
import com.uitmerch.backend.user.dto.UserProfileResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;

    @InjectMocks private UserService userService;

    private final UUID userId = UUID.randomUUID();

    private User user() {
        return User.builder()
            .id(userId).email("u@uit.edu.vn").fullName("Nguyen Van A")
            .passwordHash("h").phone("0901234567").address("HCM")
            .role(UserRole.CUSTOMER).isVerified(true).isActive(true).build();
    }

    // ── getProfile ───────────────────────────────────────────────────────────

    @Test
    void getProfile_found_returnsResponse() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user()));

        UserProfileResponse response = userService.getProfile(userId);

        assertThat(response.getId()).isEqualTo(userId);
        assertThat(response.getEmail()).isEqualTo("u@uit.edu.vn");
        assertThat(response.getFullName()).isEqualTo("Nguyen Van A");
    }

    @Test
    void getProfile_notFound_throwsResourceNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getProfile(userId))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── updateProfile ────────────────────────────────────────────────────────

    @Test
    void updateProfile_partialUpdate_onlyModifiesProvidedFields() {
        User u = user();
        when(userRepository.findById(userId)).thenReturn(Optional.of(u));
        when(userRepository.save(u)).thenReturn(u);

        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setFullName("Tran Thi B");
        // phone, address, avatarUrl not set — should remain unchanged

        UserProfileResponse response = userService.updateProfile(userId, req);

        assertThat(u.getFullName()).isEqualTo("Tran Thi B");
        assertThat(u.getPhone()).isEqualTo("0901234567"); // unchanged
        verify(userRepository).save(u);
    }

    @Test
    void updateProfile_allFields_updatesAll() {
        User u = user();
        when(userRepository.findById(userId)).thenReturn(Optional.of(u));
        when(userRepository.save(u)).thenReturn(u);

        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setFullName("New Name");
        req.setPhone("0999999999");
        req.setAddress("New Address");
        req.setAvatarUrl("https://example.com/avatar.jpg");

        userService.updateProfile(userId, req);

        assertThat(u.getFullName()).isEqualTo("New Name");
        assertThat(u.getPhone()).isEqualTo("0999999999");
        assertThat(u.getAddress()).isEqualTo("New Address");
        assertThat(u.getAvatarUrl()).isEqualTo("https://example.com/avatar.jpg");
    }

    @Test
    void updateProfile_userNotFound_throwsResourceNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateProfile(userId, new UpdateProfileRequest()))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
