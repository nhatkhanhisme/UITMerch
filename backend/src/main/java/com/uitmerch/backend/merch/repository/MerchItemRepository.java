package com.uitmerch.backend.merch.repository;

import com.uitmerch.backend.common.model.MerchItemStatus;
import com.uitmerch.backend.merch.entity.MerchItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MerchItemRepository extends JpaRepository<MerchItem, UUID> {

    Page<MerchItem> findByStatus(MerchItemStatus status, Pageable pageable);

    List<MerchItem> findAllByStatus(MerchItemStatus status);

    Page<MerchItem> findByStatusAndNameContainingIgnoreCase(MerchItemStatus status, String name, Pageable pageable);

    Page<MerchItem> findByStatusAndCategoryId(MerchItemStatus status, UUID categoryId, Pageable pageable);

    Page<MerchItem> findByStatusAndCategoryIdAndNameContainingIgnoreCase(MerchItemStatus status, UUID categoryId, String name, Pageable pageable);

    Page<MerchItem> findByOrgId(UUID orgId, Pageable pageable);

    Page<MerchItem> findByOrgIdAndStatus(UUID orgId, MerchItemStatus status, Pageable pageable);

    Optional<MerchItem> findByIdAndOrgId(UUID id, UUID orgId);

    boolean existsByIdAndOrgId(UUID id, UUID orgId);


    long countByOrgIdAndStatus(UUID orgId, MerchItemStatus status);

    @Query("SELECT m.orgId, COUNT(m) FROM MerchItem m WHERE m.orgId IN :orgIds AND m.status = :status GROUP BY m.orgId")
    List<Object[]> countByOrgIdsAndStatus(@Param("orgIds") List<UUID> orgIds, @Param("status") MerchItemStatus status);

    /**
     * Atomically deducts qty from stock only when stock >= qty.
     * Returns 1 on success, 0 if stock was insufficient (concurrent order won the race).
     */
    @Modifying
    @Query("UPDATE MerchItem m SET m.stock = m.stock - :qty WHERE m.id = :id AND m.stock >= :qty")
    int deductStock(@Param("id") UUID id, @Param("qty") int qty);

    /**
     * Archives all PUBLISHED merch for an org when the org is suspended/deactivated.
     */
    @Modifying
    @Query("UPDATE MerchItem m SET m.status = com.uitmerch.backend.common.model.MerchItemStatus.ARCHIVED WHERE m.orgId = :orgId AND m.status = com.uitmerch.backend.common.model.MerchItemStatus.PUBLISHED")
    int archivePublishedByOrgId(@Param("orgId") UUID orgId);
}
