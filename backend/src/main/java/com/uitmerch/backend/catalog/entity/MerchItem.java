package com.uitmerch.backend.catalog.entity;

import com.uitmerch.backend.organizations.entity.Organization;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Merch item entity representing sellable merchandise.
 * FR05: Organizer can create merch with is_preorder and stock_quantity.
 * FR03: Customer can search by keyword, organization.
 * BR07: Pre-order items bypass stock_quantity validation.
 * BR08: Cannot hard-delete item if linked to orders (ON DELETE RESTRICT).
 */
@Entity
@Table(name = "merch_items", indexes = {
    @Index(name = "idx_merch_items_organization_id", columnList = "organization_id"),
    @Index(name = "idx_merch_items_is_active", columnList = "is_active"),
    @Index(name = "idx_merch_items_is_preorder", columnList = "is_preorder")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;
    
    @Column(nullable = false, length = 180)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String meaningText;
    
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;
    
    @Column(nullable = false)
    private Integer stockQuantity;
    
    @Column(nullable = false)
    private Boolean isPreorder;
    
    @Column(nullable = false)
    private Boolean isActive;
    
    @Column(columnDefinition = "TEXT")
    private String imageUrl;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}
