package com.uitmerch.backend.event.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "event_merch")
@IdClass(EventMerchId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventMerch {

    @Id
    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Id
    @Column(name = "merch_id", nullable = false)
    private UUID merchId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
