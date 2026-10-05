package com.uitmerch.backend.order.security;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="guest_tracking_credentials") @Getter @Setter @NoArgsConstructor
public class GuestTrackingCredential {
    @Id private UUID orderId;
    @Column(length=64,nullable=false) private String tokenHash;
    @Column(nullable=false) private Instant expiresAt;
}
