package com.uitmerch.backend.auth.repository;

import com.uitmerch.backend.auth.entity.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, UUID> {

    Optional<EmailVerification> findByUser_EmailIgnoreCase(String email);

    void deleteByUser_EmailIgnoreCase(String email);
}
