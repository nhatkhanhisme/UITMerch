package com.uitmerch.backend.carts.entity;

import com.uitmerch.backend.common.model.CartStatus;
import com.uitmerch.backend.auth.entity.User;
import com.uitmerch.backend.organizations.entity.Organization;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Cart entity representing active shopping session.
 * FR06: Customer can add/remove items.
 * BR03: Single-organization constraint (enforced by unique constraint).
 */
@Entity
@Table(name = "carts", indexes = {
    @Index(name = "idx_carts_customer_user_id", columnList = "customer_user_id"),
    @Index(name = "idx_carts_organization_id", columnList = "organization_id"),
    @Index(name = "idx_carts_status", columnList = "status")
},
uniqueConstraints = {
    @UniqueConstraint(
        name = "one_active_cart_per_customer_org",
        columnNames = {"customer_user_id", "organization_id", "status"}
    )
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cart {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_user_id", nullable = false)
    private User customerUser;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CartStatus status;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}
