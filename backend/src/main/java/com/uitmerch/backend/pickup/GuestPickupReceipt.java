package com.uitmerch.backend.pickup;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name = "guest_pickup_receipts") @Getter @Setter
public class GuestPickupReceipt {
    @Id @Column(name = "order_id") private UUID orderId;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64) private String tokenHash;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "consumed_at") private Instant consumedAt;
}
