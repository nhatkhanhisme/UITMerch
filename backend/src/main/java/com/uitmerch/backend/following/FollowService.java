package com.uitmerch.backend.following;
import com.uitmerch.backend.auth.repository.UserRepository;
import com.uitmerch.backend.common.exception.*;
import com.uitmerch.backend.common.model.*;
import com.uitmerch.backend.organization.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;
@Service @RequiredArgsConstructor
public class FollowService {
    private final FollowRepository follows;
    private final UserRepository users;
    private final OrganizationRepository organizations;
    @Transactional
    public OrganizationFollow follow(UUID user, UUID org, FollowPreferences preferences) {
        lockUser(user);
        organizations.findById(org).filter(o -> o.getStatus() == OrganizationStatus.ACTIVE)
            .orElseThrow(() -> new ResourceNotFoundException("Organization", org.toString()));
        var row = follows.findByUserIdAndOrgId(user, org).orElseGet(OrganizationFollow::new);
        if (row.getId() == null || !row.isEnabled()) row.setFollowedAt(Instant.now());
        row.setUserId(user); row.setOrgId(org); row.setEnabled(true); apply(row, preferences);
        return follows.save(row);
    }
    @Transactional
    public OrganizationFollow preferences(UUID user, UUID org, FollowPreferences preferences) {
        lockUser(user);
        var row = follows.findByUserIdAndOrgId(user, org).filter(OrganizationFollow::isEnabled)
            .orElseThrow(() -> new ResourceNotFoundException("Follow", org.toString()));
        apply(row, preferences); return row;
    }
    @Transactional
    public void unfollow(UUID user, UUID org) {
        lockUser(user); follows.findByUserIdAndOrgId(user, org).ifPresent(f -> f.setEnabled(false));
    }
    @Transactional(readOnly = true)
    public Page<OrganizationFollow> list(UUID user, Pageable page) { return follows.findByUserIdAndEnabledTrue(user, page); }
    private void lockUser(UUID user) {
        users.findLockedById(user).filter(u -> u.isActive() && u.isVerified() && u.getRole() == UserRole.CUSTOMER)
            .orElseThrow(() -> new AuthenticationException("Invalid account"));
    }
    private void apply(OrganizationFollow row, FollowPreferences p) {
        if (p == null) return;
        if (p.getNotifyMerch() != null) row.setNotifyMerch(p.getNotifyMerch());
        if (p.getNotifyEvents() != null) row.setNotifyEvents(p.getNotifyEvents());
        if (p.getEmailEnabled() != null) row.setEmailEnabled(p.getEmailEnabled());
    }
}
