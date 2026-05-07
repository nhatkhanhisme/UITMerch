package com.uitmerch.backend.orders.entity;

import com.uitmerch.backend.common.model.OrderStatus;
import com.uitmerch.backend.common.model.PaymentMethod;
import com.uitmerch.backend.auth.entity.User;
import com.uitmerch.backend.organizations.entity.Organization;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Order entity representing COD orders.
 * FR07: Customer submits cart to create PENDING order.
 * FR08: Organizer updates order status through lifecycle.
 * BR04: idempotency_key enables idempotent checkout.
 * BR05: Payment method is exclusively COD.
 * BR06: Order status lifecycle: PENDING -> CONFIRMED -> READY_FOR_PICKUP -> SUCCESS or CANCELLED.
 * BR08: Cannot hard-delete order if referenced (ON DELETE RESTRICT for FK).
 */
@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_orders_customer_user_id", columnList = "customer_user_id"),
    @Index(name = "idx_orders_organization_id", columnList = "organization_id"),
    @Index(name = "idx_orders_status", columnList = "status"),
    @Index(name = "idx_orders_order_code", columnList = "order_code"),
    @Index(name = "idx_orders_idempotency_key", columnList = "idempotency_key"),
    @Index(name = "idx_orders_placed_at", columnList = "placed_at")
},
uniqueConstraints = {
    @UniqueConstraint(name = "uk_order_code", columnNames = {"order_code"}),
    @UniqueConstraint(name = "uk_idempotency_key", columnNames = {"idempotency_key"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false, length = 30, unique = true)
    private String orderCode;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_user_id", nullable = false)
    private User customerUser;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;
    
    @Column(nullable = false, length = 80, unique = true)
    private String idempotencyKey;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;
    
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;
    
    @Column(nullable = false)
    private Instant placedAt;
    
    @Column
    private Instant confirmedAt;
    
    @Column
    private Instant readyAt;
    
    @Column
    private Instant completedAt;
    
    @Column
    private Instant cancelledAt;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}
