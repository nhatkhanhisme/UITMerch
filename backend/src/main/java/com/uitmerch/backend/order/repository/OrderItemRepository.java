package com.uitmerch.backend.order.repository;

import com.uitmerch.backend.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    List<OrderItem> findByOrderId(UUID orderId);

    // Total quantity ordered per merch (non-cancelled orders only)
    @Query(value = """
            SELECT oi.merch_id, SUM(oi.quantity)
            FROM order_items oi
            JOIN orders o ON o.id = oi.order_id
            WHERE oi.merch_id IN :ids AND o.status <> 'CANCELLED'
            GROUP BY oi.merch_id
            """, nativeQuery = true)
    List<Object[]> sumQuantityByMerchIds(@Param("ids") List<UUID> ids);

    // Recent quantity ordered per merch (non-cancelled, within a time window)
    @Query(value = """
            SELECT oi.merch_id, SUM(oi.quantity)
            FROM order_items oi
            JOIN orders o ON o.id = oi.order_id
            WHERE oi.merch_id IN :ids AND o.status <> 'CANCELLED'
              AND oi.created_at >= :since
            GROUP BY oi.merch_id
            """, nativeQuery = true)
    List<Object[]> sumQuantityByMerchIdsSince(@Param("ids") List<UUID> ids, @Param("since") LocalDateTime since);
}
