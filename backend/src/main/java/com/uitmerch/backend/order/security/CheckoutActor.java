package com.uitmerch.backend.order.security;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="checkout_actors") @Getter @Setter @NoArgsConstructor
public class CheckoutActor {
    @Id @Column(length=80) private String actorKey;
    @Column(length=100) private String codeHash;
    private UUID challengeId;
    private Instant codeExpiresAt;
    private int failedAttempts;
    @Column(length=64) private String tokenHash;
    private Instant verifiedUntil;
}
