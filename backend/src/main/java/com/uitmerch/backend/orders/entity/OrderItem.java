package com.uitmerch.backend.orders.entity;

import com.uitmerch.backend.catalog.entity.MerchItem;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * OrderItem entity representing line items in an order.
 * Snapshot of merch item details at time of order.
 */
@Entity
@Table(name = "order_items", indexes = {
    @Index(name = "idx_order_items_order_id", columnList = "order_id"),
    @Index(name = "idx_order_items_merch_item_id", columnList = "merch_item_id")
},
uniqueConstraints = {
    @UniqueConstraint(
        name = "unique_item_per_order",
        columnNames = {"order_id", "merch_item_id"}
    )
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merch_item_id", nullable = false)
    private MerchItem merchItem;
    
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPriceSnapshot;
    
    @Column(nullable = false)
    private Integer quantity;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
