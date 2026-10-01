package com.uitmerch.backend.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auth_sessions")
@Getter @Setter @NoArgsConstructor
public class AuthSession {
    @Id private UUID id;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(name = "refresh_hash", nullable = false, length = 64) private String refreshHash;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "revoked_at") private Instant revokedAt;
}
