package com.uitmerch.backend.organizations.entity;

import com.uitmerch.backend.common.model.OrganizationStatus;
import com.uitmerch.backend.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Organization entity representing clubs/faculties.
 * FR04: Organizer can update org profile (bio, logo).
 * BR08: Cannot hard-delete org if linked to orders (ON DELETE RESTRICT).
 */
@Entity
@Table(name = "organizations", indexes = {
    @Index(name = "idx_organizations_owner_user_id", columnList = "owner_user_id"),
    @Index(name = "idx_organizations_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Organization {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private User ownerUser;
    
    @Column(nullable = false, length = 180)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(columnDefinition = "TEXT")
    private String logoUrl;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrganizationStatus status;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}
