package com.uitmerch.backend.order.repository;

import com.uitmerch.backend.common.model.OrderStatus;
import com.uitmerch.backend.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {
    java.util.List<Order> findTop100ByStatusAndPendingExpiresAtBeforeOrderByPendingExpiresAtAsc(
        OrderStatus status,java.time.Instant now);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT o FROM Order o WHERE o.id = :id")
    java.util.Optional<Order> findLockedById(@org.springframework.data.repository.query.Param("id") UUID id);

    Page<Order> findByUserId(UUID userId, Pageable pageable);

    Page<Order> findByUserIdAndStatus(UUID userId, OrderStatus status, Pageable pageable);

    Page<Order> findByOrgId(UUID orgId, Pageable pageable);

    Page<Order> findByOrgIdAndStatus(UUID orgId, OrderStatus status, Pageable pageable);

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    java.util.List<Order> findByPickupScheduleId(UUID pickupScheduleId);

    Page<Order> findByOrgIdAndPickupScheduleId(UUID orgId, UUID pickupScheduleId, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("""
        SELECT o.pickupScheduleId, COUNT(o) FROM Order o
        WHERE o.pickupScheduleId IN :scheduleIds GROUP BY o.pickupScheduleId
        """)
    java.util.List<Object[]> countByPickupScheduleIds(
        @org.springframework.data.repository.query.Param("scheduleIds") java.util.List<UUID> scheduleIds);

    long countByPickupScheduleId(UUID pickupScheduleId);
}
