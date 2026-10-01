package com.uitmerch.backend.auth.repository;

import com.uitmerch.backend.auth.entity.AuthSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface AuthSessionRepository extends JpaRepository<AuthSession, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM AuthSession s WHERE s.id = :id")
    Optional<AuthSession> findLockedById(@Param("id") UUID id);

    @Modifying
    @Query("DELETE FROM AuthSession s WHERE s.expiresAt < :now")
    void deleteExpiredBefore(@Param("now") java.time.Instant now);
}
