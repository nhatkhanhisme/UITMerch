package com.uitmerch.backend.common.repository;

import com.uitmerch.backend.common.entity.InvalidatedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface InvalidatedTokenRepository extends JpaRepository<InvalidatedToken, String> {

    boolean existsByTokenHashAndExpiresAtAfter(String tokenHash, Instant now);

    List<InvalidatedToken> findAllByExpiresAtAfter(Instant now);

    @Modifying
    @Query("DELETE FROM InvalidatedToken t WHERE t.expiresAt < :cutoff")
    void deleteExpiredBefore(@Param("cutoff") Instant cutoff);
}
