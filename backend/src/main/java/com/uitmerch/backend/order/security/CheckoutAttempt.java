package com.uitmerch.backend.order.security;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
@Entity @Table(name="checkout_attempts") @Getter @Setter @NoArgsConstructor
public class CheckoutAttempt {
    @Id @Column(length=64) private String id;
    @Column(length=80,nullable=false) private String actorKey;
    @Column(length=64,nullable=false) private String fingerprint;
    @Column(columnDefinition="TEXT") private String responseJson;
    @Column(nullable=false) private Instant createdAt;
}
