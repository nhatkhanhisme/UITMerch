package com.uitmerch.backend.common.delivery;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "background_jobs")
@Getter @Setter @NoArgsConstructor
public class BackgroundJob {
    @Id @GeneratedValue @UuidGenerator private UUID id;
    @Column(nullable = false, length = 20) private String kind;
    @Column(nullable = false, columnDefinition = "TEXT") private String payload;
    @Column(nullable = false, length = 20) private String state = "PENDING";
    @Column(nullable = false) private int attempts;
    @Column(name = "next_attempt_at", nullable = false) private Instant nextAttemptAt = Instant.now();
    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();
}
