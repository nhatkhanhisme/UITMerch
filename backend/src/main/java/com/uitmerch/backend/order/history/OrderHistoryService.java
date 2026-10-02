package com.uitmerch.backend.order.history;
import com.uitmerch.backend.common.exception.ResourceNotFoundException;
import com.uitmerch.backend.common.model.OrderStatus;
import com.uitmerch.backend.order.entity.Order;
import com.uitmerch.backend.order.repository.OrderRepository;
import com.uitmerch.backend.organization.service.OrganizationService;
import com.uitmerch.backend.pickup.PickupTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;
@Service @RequiredArgsConstructor
public class OrderHistoryService {
    private final OrderHistoryRepository history;
    private final PickupTokenRepository tokens;
    private final OrderRepository orders;
    private final OrganizationService organizations;

    @Transactional
    public void record(Order order, UUID actor, OrderStatus previous, String source) {
        OrderHistory row = new OrderHistory(); row.setId(UUID.randomUUID()); row.setOrderId(order.getId());
        row.setActorId(actor); row.setFromStatus(previous); row.setToStatus(order.getStatus()); row.setSource(source);
        row.setPickupScheduleId(order.getPickupScheduleId()); row.setCreatedAt(Instant.now()); history.save(row);
        if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.CANCELLED) {
            tokens.findById(order.getId()).ifPresent(t -> t.setRevokedAt(Instant.now()));
        }
    }
    @Transactional(readOnly = true)
    public Page<OrderHistory> forCustomer(UUID userId, UUID orderId, Pageable page) {
        orders.findById(orderId).filter(o -> userId.equals(o.getUserId())).orElseThrow(() -> missing(orderId));
        return history.findByOrderIdOrderByCreatedAtAscIdAsc(orderId, page);
    }
    @Transactional(readOnly = true)
    public Page<OrderHistory> forOrganization(UUID ownerId, UUID orgId, UUID orderId, Pageable page) {
        organizations.getOwnOrganizationEntity(ownerId, orgId);
        orders.findById(orderId).filter(o -> orgId.equals(o.getOrgId())).orElseThrow(() -> missing(orderId));
        return history.findByOrderIdOrderByCreatedAtAscIdAsc(orderId, page);
    }
    private ResourceNotFoundException missing(UUID id) { return new ResourceNotFoundException("Order", id.toString()); }
}
