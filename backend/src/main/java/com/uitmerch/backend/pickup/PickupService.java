package com.uitmerch.backend.pickup;

import com.uitmerch.backend.common.delivery.BackgroundJobService;
import com.uitmerch.backend.common.exception.*;
import com.uitmerch.backend.common.model.OrderStatus;
import com.uitmerch.backend.order.dto.OrderResponse;
import com.uitmerch.backend.order.entity.Order;
import com.uitmerch.backend.order.history.OrderHistoryService;
import com.uitmerch.backend.order.repository.*;
import com.uitmerch.backend.order.service.OrderService;
import com.uitmerch.backend.organization.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor
public class PickupService {
    private final PickupTokenRepository tokens;
    private final GuestPickupReceiptRepository receipts;
    private final OrderRepository orders;
    private final OrderService orderService;
    private final OrganizationService organizations;
    private final PickupScheduleRepository schedules;
    private final OrderHistoryService history;
    private final BackgroundJobService jobs;
    public record IssuedToken(UUID orderId, String token, UUID pickupScheduleId, Instant expiresAt) {}

    @Transactional
    public IssuedToken issue(UUID userId, UUID orderId) {
        Order order = locked(orderId);
        if (!userId.equals(order.getUserId())) throw new ResourceNotFoundException("Order", orderId.toString());
        return issueForOrder(order, userId, "CUSTOMER");
    }
    @Transactional
    public void requestGuestReceipt(UUID orderId, String email) {
        var candidate = orders.findLockedById(orderId);
        if (candidate.isEmpty()) return;
        var order = candidate.get();
        if (order.getUserId() != null || order.getStatus() != OrderStatus.READY || order.getGuestEmail() == null
            || !order.getGuestEmail().equalsIgnoreCase(email.trim())) return;
        String credential = PickupCredentials.generate();
        var receipt = receipts.findById(orderId).orElseGet(GuestPickupReceipt::new);
        receipt.setOrderId(orderId); receipt.setTokenHash(PickupCredentials.hash(credential));
        receipt.setExpiresAt(Instant.now().plusSeconds(900)); receipt.setConsumedAt(null); receipts.save(receipt);
        jobs.enqueueEmail("PICKUP_RECEIPT", order.getGuestEmail(), credential, orderId.toString().substring(0, 8));
    }
    @Transactional
    public IssuedToken issueForGuest(UUID orderId, String receiptToken) {
        Order order = locked(orderId);
        var receipt = receipts.findById(orderId).orElseThrow(PickupCredentials::invalid);
        if (order.getUserId() != null || receipt.getConsumedAt() != null || !receipt.getExpiresAt().isAfter(Instant.now())
            || !PickupCredentials.matches(receiptToken, receipt.getTokenHash())) throw PickupCredentials.invalid();
        receipt.setConsumedAt(Instant.now());
        return issueForOrder(order, null, "GUEST");
    }
    private IssuedToken issueForOrder(Order order, UUID actor, String source) {
        if (order.getStatus() != OrderStatus.READY) throw new ValidationException("Only READY orders have pickup tokens.");
        String credential = PickupCredentials.generate();
        var token = tokens.findById(order.getId()).orElseGet(PickupToken::new);
        token.setOrderId(order.getId()); token.setTokenHash(PickupCredentials.hash(credential));
        token.setPickupScheduleId(order.getPickupScheduleId()); token.setExpiresAt(Instant.now().plusSeconds(1800));
        token.setConsumedAt(null); token.setRevokedAt(null); tokens.save(token);
        history.record(order, actor, OrderStatus.READY, source);
        return new IssuedToken(order.getId(), credential, token.getPickupScheduleId(), token.getExpiresAt());
    }
    @Transactional(readOnly = true)
    public OrderResponse verify(UUID ownerId, UUID orgId, String credential, UUID scheduleId) {
        organizations.getOwnOrganizationEntity(ownerId, orgId);
        var token = tokens.findByTokenHash(PickupCredentials.hash(credential)).orElseThrow(PickupCredentials::invalid);
        var order = orders.findById(token.getOrderId()).orElseThrow(PickupCredentials::invalid);
        validate(order, token, orgId, scheduleId, credential);
        return orderService.getOrgOrder(ownerId, orgId, order.getId());
    }
    @Transactional
    public OrderResponse checkIn(UUID ownerId, UUID orgId, String credential, UUID scheduleId) {
        organizations.getOwnOrganizationEntity(ownerId, orgId);
        var orderId = tokens.findOrderIdByTokenHash(PickupCredentials.hash(credential)).orElseThrow(PickupCredentials::invalid);
        // Scalar lookup avoids caching a token that could be replaced while waiting for the order lock.
        Order order = locked(orderId); // All issuance/check-in paths lock order before token.
        var token = tokens.findById(order.getId()).orElseThrow(PickupCredentials::invalid);
        validate(order, token, orgId, scheduleId, credential);
        token.setConsumedAt(Instant.now());
        return orderService.completePickup(ownerId, orgId, order.getId(), "QR");
    }
    private void validate(Order order, PickupToken token, UUID orgId, UUID scheduleId, String credential) {
        if (!orgId.equals(order.getOrgId()) || order.getStatus() != OrderStatus.READY
            || token.getConsumedAt() != null || token.getRevokedAt() != null || !token.getExpiresAt().isAfter(Instant.now())
            || !Objects.equals(order.getPickupScheduleId(), token.getPickupScheduleId())
            || !Objects.equals(scheduleId, token.getPickupScheduleId())
            || !PickupCredentials.matches(credential, token.getTokenHash())) throw PickupCredentials.invalid();
        if (scheduleId != null) {
            var schedule = schedules.findById(scheduleId).orElseThrow(PickupCredentials::invalid);
            if (!orgId.equals(schedule.getOrgId()) || LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).isBefore(schedule.getPickupDate()))
                throw new ValidationException("Pickup schedule is not available yet.");
        }
    }
    private Order locked(UUID id) { return orders.findLockedById(id).orElseThrow(() -> new ResourceNotFoundException("Order", id.toString())); }
}
