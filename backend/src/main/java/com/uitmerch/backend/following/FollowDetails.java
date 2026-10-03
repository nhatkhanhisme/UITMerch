package com.uitmerch.backend.following;

import com.uitmerch.backend.common.model.OrganizationStatus;
import java.time.Instant;
import java.util.UUID;

/** Display context for the authenticated customer's own follows; no follower identities. */
public record FollowDetails(UUID orgId, String orgName, String logoUrl, OrganizationStatus orgStatus,
    boolean notifyMerch, boolean notifyEvents, boolean emailEnabled, Instant followedAt) {}
