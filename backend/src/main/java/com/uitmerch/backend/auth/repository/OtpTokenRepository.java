package com.uitmerch.backend.auth.repository;

import com.uitmerch.backend.auth.entity.OtpToken;
import com.uitmerch.backend.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OtpTokenRepository extends JpaRepository<OtpToken, UUID> {

    Optional<OtpToken> findTopByUserAndOtpCodeAndIsUsedFalseOrderByCreatedAtDesc(
            User user, String otpCode);

    void deleteAllByUser(User user);

    Optional<OtpToken> findTopByUserOrderByCreatedAtDesc(User user);
}
