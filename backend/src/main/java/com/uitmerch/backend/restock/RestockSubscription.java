package com.uitmerch.backend.restock;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "restock_subscriptions", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "merch_id"}))
@Getter @Setter
public class RestockSubscription {
    @Id @GeneratedValue @UuidGenerator private UUID id;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(name = "merch_id", nullable = false) private UUID merchId;
    @Column(nullable = false) private boolean enabled = true;
    @Column(name = "email_enabled", nullable = false) private boolean emailEnabled = true;
    @Column(name = "subscribed_at", nullable = false) private Instant subscribedAt;
}
