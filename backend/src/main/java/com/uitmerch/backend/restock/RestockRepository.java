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
        SELECT new com.uitmerch.backend.restock.RestockDetails(s.merchId, m.name, o.name,
            CASE WHEN m.stock > 0 AND m.status = :#{T(com.uitmerch.backend.common.model.MerchItemStatus).PUBLISHED}
                AND o.status = :#{T(com.uitmerch.backend.common.model.OrganizationStatus).ACTIVE} THEN true ELSE false END,
            s.emailEnabled, s.subscribedAt)
        FROM RestockSubscription s JOIN MerchItem m ON m.id = s.merchId JOIN Organization o ON o.id = m.orgId
        WHERE s.userId = :userId AND s.enabled = true
        """)
    Page<RestockDetails> findDetails(@Param("userId") UUID userId, Pageable page);
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
