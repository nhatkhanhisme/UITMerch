package com.uitmerch.backend.order.repository;

import com.uitmerch.backend.order.entity.PickupSchedule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PickupScheduleRepository extends JpaRepository<PickupSchedule, UUID> {
    Page<PickupSchedule> findByOrgIdOrderByPickupDateDesc(UUID orgId, Pageable pageable);
    long countByOrgId(UUID orgId);
}
