package com.uitmerch.backend.collections.entity;

import com.uitmerch.backend.auth.entity.User;
import com.uitmerch.backend.catalog.entity.MerchItem;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Collection entity representing digital gallery of successful purchases.
 * FR09: Customer can view historical gallery of purchased items.
 * BR09: Only orders with SUCCESS status are added to Collection (enforced in app layer).
 */
@Entity
@Table(name = "collections", indexes = {
    @Index(name = "idx_collections_customer_user_id", columnList = "customer_user_id"),
    @Index(name = "idx_collections_merch_item_id", columnList = "merch_item_id"),
    @Index(name = "idx_collections_acquired_at", columnList = "acquired_at")
},
uniqueConstraints = {
    @UniqueConstraint(
        name = "unique_customer_item_collection",
        columnNames = {"customer_user_id", "merch_item_id"}
    )
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Collection {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_user_id", nullable = false)
    private User customerUser;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merch_item_id", nullable = false)
    private MerchItem merchItem;
    
    @Column(nullable = false)
    private Instant acquiredAt;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
