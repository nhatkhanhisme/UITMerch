package com.uitmerch.backend.restock;

import com.uitmerch.backend.notification.announcement.AnnouncementAudience;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.*;

public interface RestockRepository extends JpaRepository<RestockSubscription, UUID> {
    Optional<RestockSubscription> findByUserIdAndMerchId(UUID userId, UUID merchId);
    Page<RestockSubscription> findByUserIdAndEnabledTrue(UUID userId, Pageable page);
    @Query("""
        SELECT s.userId AS userId, u.email AS email, s.emailEnabled AS emailEnabled
        FROM RestockSubscription s JOIN User u ON u.id = s.userId
        WHERE s.merchId = :merchId AND s.enabled = true AND s.subscribedAt <= :createdAt
        AND u.isActive = true AND u.isVerified = true
        AND (:cursor IS NULL OR s.userId > :cursor) ORDER BY s.userId
        """)
    List<AnnouncementAudience.Recipient> audience(@Param("merchId") UUID merchId,
        @Param("createdAt") Instant createdAt, @Param("cursor") UUID cursor, Pageable page);
}
