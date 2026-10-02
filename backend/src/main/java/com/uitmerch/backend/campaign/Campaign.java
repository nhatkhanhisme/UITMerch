package com.uitmerch.backend.campaign;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="preorder_campaigns") @Getter @Setter @NoArgsConstructor
public class Campaign {
    public enum State { ACTIVE, SUCCEEDED, FAILED, CANCELLED }
    @Id private UUID id;
    @Column(name="org_id",nullable=false) private UUID orgId;
    @Column(nullable=false) private String title;
    @Column(length=4000) private String description;
    @Column(name="minimum_quantity",nullable=false) private int minimumQuantity;
    @Column(nullable=false) private Instant deadline;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=16) private State state;
    @Column(name="closed_quantity") private Long closedQuantity;
    @Column(name="created_at",nullable=false) private Instant createdAt;
}
