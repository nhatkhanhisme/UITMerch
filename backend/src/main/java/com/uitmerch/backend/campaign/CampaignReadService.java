package com.uitmerch.backend.campaign;

import com.uitmerch.backend.common.exception.ResourceNotFoundException;
import com.uitmerch.backend.common.model.MerchItemStatus;
import com.uitmerch.backend.common.model.OrganizationStatus;
import com.uitmerch.backend.merch.repository.MerchItemRepository;
import com.uitmerch.backend.order.repository.OrderRepository;
import com.uitmerch.backend.organization.repository.OrganizationRepository;
import com.uitmerch.backend.organization.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Read contracts for storefront routing and owner-only fulfillment decisions. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CampaignReadService {
    private final CampaignRepository campaigns;
    private final CampaignVariantRepository variants;
    private final CampaignReservationRepository reservations;
    private final MerchItemRepository merchandise;
    private final OrderRepository orders;
    private final OrganizationRepository orgRepository;
    private final OrganizationService organizations;
    private final NamedParameterJdbcTemplate jdbc;

    public record PurchaseContext(UUID campaignId, boolean reservationRequired) {}
    public record OrderContext(UUID campaignId, Campaign.State campaignState, boolean fulfillmentAllowed) {}

    public PurchaseContext purchaseContext(UUID merchId) {
        var merch = merchandise.findById(merchId)
                .filter(m -> m.getStatus() == MerchItemStatus.PUBLISHED && activeOrg(m.getOrgId()))
                .orElseThrow(() -> new ResourceNotFoundException("Merchandise", merchId.toString()));
        // An overdue ACTIVE campaign still blocks ordinary checkout until its closer has run.
        var active = variants.activeCampaigns(merch.getId());
        return new PurchaseContext(active.isEmpty() ? null : active.getFirst(), !active.isEmpty());
    }

    public OrderContext customerOrder(UUID userId, UUID orderId) {
        orders.findById(orderId).filter(o -> userId.equals(o.getUserId()))
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId.toString()));
        return context(orderId);
    }

    public OrderContext organizationOrder(UUID ownerId, UUID orgId, UUID orderId) {
        organizations.getOwnOrganizationEntity(ownerId, orgId);
        orders.findById(orderId).filter(o -> orgId.equals(o.getOrgId()))
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId.toString()));
        return context(orderId);
    }

    public CampaignResponse organizationDetail(UUID ownerId, UUID orgId, UUID campaignId) {
        organizations.getOwnOrganizationEntity(ownerId, orgId);
        var c = campaigns.findById(campaignId).filter(row -> orgId.equals(row.getOrgId()))
                .orElseThrow(() -> new ResourceNotFoundException("Campaign", campaignId.toString()));
        var specs = variants.findByCampaignIdOrderById(campaignId);
        var products = merchandise.findAllById(specs.stream().map(CampaignVariant::getMerchId).toList())
                .stream().collect(Collectors.toMap(m -> m.getId(), m -> m));
        boolean open = c.getState() == Campaign.State.ACTIVE && c.getDeadline().isAfter(Instant.now()) && activeOrg(orgId);
        var details = specs.stream().map(v -> {
            var m = products.get(v.getMerchId());
            boolean available = open && m != null && orgId.equals(m.getOrgId()) && m.getStatus() == MerchItemStatus.PUBLISHED;
            return new CampaignResponse.Variant(v.getMerchId(), v.getLabel(), v.getUnitPrice(),
                    available ? m.getStock() : 0, available && m.getStock() > 0);
        }).toList();
        Long quantity = c.getClosedQuantity();
        if (quantity == null) quantity = jdbc.queryForObject("""
                SELECT COALESCE(SUM(r.quantity), 0) FROM campaign_reservations r
                JOIN orders o ON o.id = r.order_id WHERE r.campaign_id = :id AND o.status <> 'CANCELLED'
                """, Map.of("id", campaignId), Long.class);
        return CampaignResponse.from(c, quantity == null ? 0 : quantity, details);
    }

    private OrderContext context(UUID orderId) {
        var reservation = reservations.findByOrderId(orderId);
        if (reservation.isEmpty()) return new OrderContext(null, null, true);
        var c = campaigns.findById(reservation.get().getCampaignId())
                .orElseThrow(() -> new ResourceNotFoundException("Campaign", reservation.get().getCampaignId().toString()));
        return new OrderContext(c.getId(), c.getState(), c.getState() == Campaign.State.SUCCEEDED);
    }

    private boolean activeOrg(UUID orgId) {
        return orgRepository.findById(orgId).map(o -> o.getStatus() == OrganizationStatus.ACTIVE).orElse(false);
    }
}
