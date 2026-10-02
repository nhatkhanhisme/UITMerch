package com.uitmerch.backend.notification.announcement;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface AnnouncementRepository extends JpaRepository<AnnouncementEvent, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AnnouncementEvent a WHERE a.id = :id")
    Optional<AnnouncementEvent> findLockedById(@Param("id") UUID id);
    boolean existsByDedupeKey(String key);
}
