package com.uitmerch.backend.carts.entity;

import com.uitmerch.backend.catalog.entity.MerchItem;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * CartItem entity representing line items in a cart.
 */
@Entity
@Table(name = "cart_items", indexes = {
    @Index(name = "idx_cart_items_cart_id", columnList = "cart_id"),
    @Index(name = "idx_cart_items_merch_item_id", columnList = "merch_item_id")
},
uniqueConstraints = {
    @UniqueConstraint(
        name = "unique_item_per_cart",
        columnNames = {"cart_id", "merch_item_id"}
    )
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merch_item_id", nullable = false)
    private MerchItem merchItem;
    
    @Column(nullable = false)
    private Integer quantity;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}
