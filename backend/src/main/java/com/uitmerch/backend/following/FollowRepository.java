package com.uitmerch.backend.following;
import com.uitmerch.backend.notification.announcement.AnnouncementAudience;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.*;
public interface FollowRepository extends JpaRepository<OrganizationFollow, UUID> {
    Optional<OrganizationFollow> findByUserIdAndOrgId(UUID userId, UUID orgId);
    Page<OrganizationFollow> findByUserIdAndEnabledTrue(UUID userId, Pageable page);
    @Query("""
        SELECT new com.uitmerch.backend.following.FollowDetails(
            f.orgId, o.name, o.logoUrl, o.status, f.notifyMerch, f.notifyEvents, f.emailEnabled, f.followedAt)
        FROM OrganizationFollow f JOIN Organization o ON o.id = f.orgId
        WHERE f.userId = :userId AND f.enabled = true
        """)
    Page<FollowDetails> findDetails(@Param("userId") UUID userId, Pageable page);
    @Query("""
        SELECT f.orgId, COUNT(f) FROM OrganizationFollow f JOIN User u ON u.id = f.userId
        WHERE f.orgId IN :orgIds AND f.enabled = true AND u.isActive = true AND u.isVerified = true
        AND u.role = :#{T(com.uitmerch.backend.common.model.UserRole).CUSTOMER}
        GROUP BY f.orgId
        """)
    List<Object[]> countFollowers(@Param("orgIds") Collection<UUID> orgIds);
    @Query("""
        SELECT f.userId AS userId, u.email AS email, f.emailEnabled AS emailEnabled
        FROM OrganizationFollow f JOIN User u ON u.id = f.userId
        WHERE f.orgId = :orgId AND f.enabled = true AND f.followedAt <= :createdAt
        AND u.isActive = true AND u.isVerified = true
        AND ((:merch = true AND f.notifyMerch = true) OR (:merch = false AND f.notifyEvents = true))
        AND (:cursor IS NULL OR f.userId > :cursor) ORDER BY f.userId
        """)
    List<AnnouncementAudience.Recipient> audience(@Param("orgId") UUID orgId, @Param("createdAt") Instant createdAt,
        @Param("cursor") UUID cursor, @Param("merch") boolean merch, Pageable page);
}
