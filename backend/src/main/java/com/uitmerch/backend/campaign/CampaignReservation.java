package com.uitmerch.backend.campaign;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="campaign_reservations",uniqueConstraints=@UniqueConstraint(columnNames={"user_id","request_id"}))
@Getter @Setter @NoArgsConstructor
public class CampaignReservation {
    @Id private UUID id;
    @Column(name="campaign_id",nullable=false) private UUID campaignId;
    @Column(name="user_id",nullable=false) private UUID userId;
    @Column(name="merch_id",nullable=false) private UUID merchId;
    @Column(name="order_id",nullable=false,unique=true) private UUID orderId;
    @Column(nullable=false) private int quantity;
    @Column(name="request_id",nullable=false) private UUID requestId;
    @Column(length=1000) private String note;
    @Column(name="created_at",nullable=false) private Instant createdAt;
}
