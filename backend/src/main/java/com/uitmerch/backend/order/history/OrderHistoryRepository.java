package com.uitmerch.backend.order.history;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface OrderHistoryRepository extends JpaRepository<OrderHistory, UUID> {
    Page<OrderHistory> findByOrderIdOrderByCreatedAtAscIdAsc(UUID id, Pageable page);
}
