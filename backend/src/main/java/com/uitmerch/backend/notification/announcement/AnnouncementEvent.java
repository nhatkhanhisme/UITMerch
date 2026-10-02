package com.uitmerch.backend.notification.announcement;

import com.uitmerch.backend.common.model.NotificationType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "announcement_events")
@Getter @Setter
public class AnnouncementEvent {
    @Id private UUID id;
    @Column(name = "dedupe_key", nullable = false, unique = true, length = 200) private String dedupeKey;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 60) private NotificationType kind;
    @Column(nullable = false) private String title;
    @Column(nullable = false, columnDefinition = "TEXT") private String message;
    @Column(name = "org_id", nullable = false) private UUID orgId;
    @Column(name = "merch_id") private UUID merchId;
    @Column(name = "event_id") private UUID eventId;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "cursor_user_id") private UUID cursorUserId;
    @Column(nullable = false) private boolean finished;
}
