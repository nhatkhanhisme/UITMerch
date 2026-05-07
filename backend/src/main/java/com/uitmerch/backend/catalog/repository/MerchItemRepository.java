package com.uitmerch.backend.catalog.repository;

import com.uitmerch.backend.catalog.entity.MerchItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository for merch catalog items.
 */
@Repository
public interface MerchItemRepository extends JpaRepository<MerchItem, UUID>, JpaSpecificationExecutor<MerchItem> {
}