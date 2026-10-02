package com.uitmerch.backend.following;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name = "organization_follows", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "org_id"}))
@Getter @Setter
public class OrganizationFollow {
    @Id @GeneratedValue @UuidGenerator private UUID id;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(name = "org_id", nullable = false) private UUID orgId;
    @Column(nullable = false) private boolean enabled = true;
    @Column(name = "notify_merch", nullable = false) private boolean notifyMerch = true;
    @Column(name = "notify_events", nullable = false) private boolean notifyEvents = true;
    @Column(name = "email_enabled", nullable = false) private boolean emailEnabled;
    @Column(name = "followed_at", nullable = false) private Instant followedAt;
}
