package com.uitmerch.backend.common.delivery;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.*;

public interface BackgroundJobRepository extends JpaRepository<BackgroundJob, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT j FROM BackgroundJob j WHERE j.state IN ('PENDING', 'PROCESSING') AND j.nextAttemptAt <= :now ORDER BY j.createdAt, j.id")
    List<BackgroundJob> findDue(@Param("now") Instant now, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT j FROM BackgroundJob j WHERE j.id = :id")
    Optional<BackgroundJob> findLockedById(@Param("id") UUID id);

    @Modifying
    @Query("DELETE FROM BackgroundJob j WHERE j.state IN ('DONE', 'DEAD') AND j.createdAt < :cutoff")
    void deleteFinishedBefore(@Param("cutoff") Instant cutoff);
}
