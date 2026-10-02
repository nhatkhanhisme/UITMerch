package com.uitmerch.backend.order.history;
import com.uitmerch.backend.common.model.OrderStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name = "order_history") @Getter @Setter
public class OrderHistory {
    @Id private UUID id;
    @Column(name = "order_id", nullable = false) private UUID orderId;
    @Column(name = "actor_id") private UUID actorId;
    @Enumerated(EnumType.STRING) @Column(name = "from_status", nullable = false) private OrderStatus fromStatus;
    @Enumerated(EnumType.STRING) @Column(name = "to_status", nullable = false) private OrderStatus toStatus;
    @Column(nullable = false, length = 30) private String source;
    @Column(name = "pickup_schedule_id") private UUID pickupScheduleId;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
}
