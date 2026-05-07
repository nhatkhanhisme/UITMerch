package com.uitmerch.backend.collections.repository;

import com.uitmerch.backend.collections.entity.Collection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for collection entities.
 * Collections represent digital gallery of successfully purchased merch items.
 */
@Repository
public interface CollectionRepository extends JpaRepository<Collection, UUID> {

    /**
     * Find if a customer already collected a specific merch item.
     */
    Optional<Collection> findByCustomerUserIdAndMerchItemId(UUID customerId, UUID merchItemId);

    /**
     * Find all collections for a customer, ordered by acquisition date.
     */
    Page<Collection> findByCustomerUserIdOrderByAcquiredAtDesc(UUID customerId, Pageable pageable);

    /**
     * Count total collections for a customer.
     */
    long countByCustomerUserId(UUID customerId);
}
