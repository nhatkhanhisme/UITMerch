package com.uitmerch.backend.auth.repository;

import com.uitmerch.backend.auth.entity.OtpToken;
import com.uitmerch.backend.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OtpTokenRepository extends JpaRepository<OtpToken, UUID> {

    Optional<OtpToken> findTopByUserAndIsUsedFalseOrderByCreatedAtDesc(User user);

    void deleteAllByUser(User user);

    @Modifying
    @Query("DELETE FROM OtpToken o WHERE o.expiresAt < :cutoff")
    void deleteExpiredBefore(@Param("cutoff") LocalDateTime cutoff);
}
