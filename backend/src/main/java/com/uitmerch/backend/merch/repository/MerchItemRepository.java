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

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM MerchItem m WHERE m.id = :id")
    Optional<MerchItem> findLockedById(@Param("id") UUID id);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM MerchItem m WHERE m.id IN :ids ORDER BY m.id")
    List<MerchItem> findAllLockedByIds(@Param("ids") List<UUID> ids);

    @Query("SELECT m FROM MerchItem m WHERE m.status = :status AND EXISTS (SELECT o.id FROM Organization o WHERE o.id = m.orgId AND o.status = :#{T(com.uitmerch.backend.common.model.OrganizationStatus).ACTIVE})")
    Page<MerchItem> findByStatus(MerchItemStatus status, Pageable pageable);

    List<MerchItem> findAllByStatus(MerchItemStatus status);

    @Query("SELECT m FROM MerchItem m WHERE m.status = :status AND EXISTS (SELECT o.id FROM Organization o WHERE o.id = m.orgId AND o.status = :#{T(com.uitmerch.backend.common.model.OrganizationStatus).ACTIVE})")
    Page<MerchItem> findAllByStatus(MerchItemStatus status, Pageable pageable);

    @Query("SELECT m FROM MerchItem m WHERE m.status = :status AND LOWER(m.name) LIKE LOWER(CONCAT('%', :name, '%')) AND EXISTS (SELECT o.id FROM Organization o WHERE o.id = m.orgId AND o.status = :#{T(com.uitmerch.backend.common.model.OrganizationStatus).ACTIVE})")
    Page<MerchItem> findByStatusAndNameContainingIgnoreCase(MerchItemStatus status, String name, Pageable pageable);

    @Query("SELECT m FROM MerchItem m WHERE m.status = :status AND m.categoryId = :categoryId AND EXISTS (SELECT o.id FROM Organization o WHERE o.id = m.orgId AND o.status = :#{T(com.uitmerch.backend.common.model.OrganizationStatus).ACTIVE})")
    Page<MerchItem> findByStatusAndCategoryId(MerchItemStatus status, UUID categoryId, Pageable pageable);

    @Query("SELECT m FROM MerchItem m WHERE m.status = :status AND m.categoryId = :categoryId AND LOWER(m.name) LIKE LOWER(CONCAT('%', :name, '%')) AND EXISTS (SELECT o.id FROM Organization o WHERE o.id = m.orgId AND o.status = :#{T(com.uitmerch.backend.common.model.OrganizationStatus).ACTIVE})")
    Page<MerchItem> findByStatusAndCategoryIdAndNameContainingIgnoreCase(MerchItemStatus status, UUID categoryId, String name, Pageable pageable);

    Page<MerchItem> findByOrgId(UUID orgId, Pageable pageable);

    @Query("SELECT m FROM MerchItem m WHERE m.orgId = :orgId AND m.status = :status AND EXISTS (SELECT o.id FROM Organization o WHERE o.id = m.orgId AND o.status = :#{T(com.uitmerch.backend.common.model.OrganizationStatus).ACTIVE})")
    Page<MerchItem> findByOrgIdAndStatus(UUID orgId, MerchItemStatus status, Pageable pageable);

    Optional<MerchItem> findByIdAndOrgId(UUID id, UUID orgId);

    @Query("SELECT m FROM MerchItem m WHERE m.id IN :ids AND m.status = :#{T(com.uitmerch.backend.common.model.MerchItemStatus).PUBLISHED} AND EXISTS (SELECT o.id FROM Organization o WHERE o.id = m.orgId AND o.status = :#{T(com.uitmerch.backend.common.model.OrganizationStatus).ACTIVE})")
    List<MerchItem> findPublicByIds(@Param("ids") List<UUID> ids);

    boolean existsByIdAndOrgId(UUID id, UUID orgId);


    long countByOrgIdAndStatus(UUID orgId, MerchItemStatus status);

    @Query("SELECT m.orgId, COUNT(m) FROM MerchItem m WHERE m.orgId IN :orgIds AND m.status = :status GROUP BY m.orgId")
    List<Object[]> countByOrgIdsAndStatus(@Param("orgIds") List<UUID> orgIds, @Param("status") MerchItemStatus status);

    /**
     * Atomically deducts qty from stock only when stock >= qty.
     * Returns 1 on success, 0 if stock was insufficient (concurrent order won the race).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE MerchItem m SET m.stock = m.stock - :qty WHERE m.id = :id AND :qty > 0 AND m.stock >= :qty AND m.status = :#{T(com.uitmerch.backend.common.model.MerchItemStatus).PUBLISHED} AND EXISTS (SELECT o.id FROM Organization o WHERE o.id = m.orgId AND o.status = :#{T(com.uitmerch.backend.common.model.OrganizationStatus).ACTIVE})")
    int deductStock(@Param("id") UUID id, @Param("qty") int qty);

    /**
     * Archives all PUBLISHED merch for an org when the org is suspended/deactivated.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE MerchItem m SET m.status = :#{T(com.uitmerch.backend.common.model.MerchItemStatus).ARCHIVED} WHERE m.orgId = :orgId AND m.status = :#{T(com.uitmerch.backend.common.model.MerchItemStatus).PUBLISHED}")
    int archivePublishedByOrgId(@Param("orgId") UUID orgId);

    /**
     * Restores stock after an order is cancelled.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE MerchItem m SET m.stock = m.stock + :qty WHERE m.id = :id AND :qty > 0")
    void restoreStock(@Param("id") UUID id, @Param("qty") int qty);
}
