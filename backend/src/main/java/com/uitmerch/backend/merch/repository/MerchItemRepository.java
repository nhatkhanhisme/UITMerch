package com.uitmerch.backend.merch.repository;

import com.uitmerch.backend.common.model.MerchItemStatus;
import com.uitmerch.backend.merch.entity.MerchItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MerchItemRepository extends JpaRepository<MerchItem, UUID> {

    Page<MerchItem> findByStatus(MerchItemStatus status, Pageable pageable);

    Page<MerchItem> findByStatusAndNameContainingIgnoreCase(MerchItemStatus status, String name, Pageable pageable);

    Page<MerchItem> findByOrgId(UUID orgId, Pageable pageable);

    Page<MerchItem> findByOrgIdAndStatus(UUID orgId, MerchItemStatus status, Pageable pageable);

    Optional<MerchItem> findByIdAndOrgId(UUID id, UUID orgId);

    List<MerchItem> findTop10ByStatusOrderByCreatedAtDesc(MerchItemStatus status);

    boolean existsByIdAndOrgId(UUID id, UUID orgId);
}
