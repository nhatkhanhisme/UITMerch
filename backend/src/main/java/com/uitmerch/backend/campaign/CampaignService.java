package com.uitmerch.backend.campaign;

import com.uitmerch.backend.auth.repository.UserRepository;
import com.uitmerch.backend.common.exception.*;
import com.uitmerch.backend.common.model.*;
import com.uitmerch.backend.merch.entity.MerchItem;
import com.uitmerch.backend.merch.repository.MerchItemRepository;
import com.uitmerch.backend.order.dto.InstantOrderRequest;
import com.uitmerch.backend.order.dto.OrderResponse;
import com.uitmerch.backend.order.repository.OrderRepository;
import com.uitmerch.backend.order.service.OrderService;
import com.uitmerch.backend.organization.repository.OrganizationRepository;
import com.uitmerch.backend.organization.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
public class CampaignService {
    private final CampaignRepository campaigns;
    private final CampaignVariantRepository variants;
    private final CampaignReservationRepository reservations;
    private final MerchItemRepository merchandise;
    private final OrganizationService organizations;
    private final OrganizationRepository orgRepository;
    private final UserRepository users;
    private final OrderService orders;
    private final OrderRepository orderRepository;
    private final NamedParameterJdbcTemplate jdbc;

    @Transactional
    public CampaignResponse create(UUID owner,UUID org,CampaignRequests.Create request) {
        if(organizations.getOwnOrganizationEntity(owner,org).getStatus()!=OrganizationStatus.ACTIVE)
            throw new ValidationException("Organization must be active.");
        if(request.title()==null || request.title().isBlank() || request.title().length()>255
                || request.minimumQuantity()<1 || request.minimumQuantity()>100000 || request.deadline()==null
                || !request.deadline().isAfter(Instant.now()) || request.deadline().isAfter(Instant.now().plus(90,ChronoUnit.DAYS))
                || request.variants()==null || request.variants().isEmpty() || request.variants().size()>20)
            throw new ValidationException("Campaign requires a future deadline within 90 days, a positive threshold and 1-20 variants.");
        List<UUID> ids=request.variants().stream().map(CampaignRequests.Variant::merchId).toList();
        if(new HashSet<>(ids).size()!=ids.size()) throw new ValidationException("Campaign variants must use distinct merchandise.");
        var locked=merchandise.findAllLockedByIds(ids);
        if(locked.size()!=ids.size() || locked.stream().anyMatch(m->!org.equals(m.getOrgId()) || m.getStatus()!=MerchItemStatus.PUBLISHED))
            throw new ValidationException("Variants must be published merchandise belonging to this organization.");
        for(UUID id:ids) if(!variants.activeCampaigns(id).isEmpty()) throw new ValidationException("A variant already belongs to an active campaign.");
        if(locked.stream().mapToLong(MerchItem::getStock).sum()<request.minimumQuantity())
            throw new ValidationException("The campaign threshold exceeds available reservation capacity.");
        Campaign campaign=new Campaign(); campaign.setId(UUID.randomUUID()); campaign.setOrgId(org);
        campaign.setTitle(request.title().trim()); campaign.setDescription(request.description());
        campaign.setMinimumQuantity(request.minimumQuantity()); campaign.setDeadline(request.deadline());
        campaign.setState(Campaign.State.ACTIVE); campaign.setCreatedAt(Instant.now()); campaigns.saveAndFlush(campaign);
        var byId=locked.stream().collect(Collectors.toMap(MerchItem::getId,m->m));
        for(var spec:request.variants()) {
            if(spec.label()==null || spec.label().isBlank() || spec.label().length()>128) throw new ValidationException("Variant label is required (maximum 128 characters).");
            CampaignVariant row=new CampaignVariant(); row.setId(UUID.randomUUID()); row.setCampaignId(campaign.getId());
            row.setMerchId(spec.merchId()); row.setLabel(spec.label().trim()); row.setUnitPrice(byId.get(spec.merchId()).getPrice()); variants.save(row);
        }
        variants.flush(); return detail(campaign);
    }
    public record ReservationResponse(UUID id,UUID campaignId,UUID orderId,UUID merchId,int quantity,Instant createdAt) {
        static ReservationResponse from(CampaignReservation r) { return new ReservationResponse(r.getId(),r.getCampaignId(),r.getOrderId(),r.getMerchId(),r.getQuantity(),r.getCreatedAt()); }
    }
    public record ReservedOrder(ReservationResponse reservation,OrderResponse order) {}
    @Transactional
    public ReservedOrder reserve(UUID user,UUID campaignId,CampaignRequests.Reserve request) {
        users.findById(user).filter(u->u.isActive() && u.isVerified() && u.getRole()==UserRole.CUSTOMER)
            .orElseThrow(()->new AuthenticationException("Invalid account"));
        Campaign campaign=lock(campaignId);
        if(request.requestId()==null || request.merchId()==null || request.quantity()<1 || request.quantity()>100
                || (request.note()!=null && request.note().length()>1000)) throw new ValidationException("Invalid reservation request.");
        var previous=reservations.findByUserIdAndRequestId(user,request.requestId());
        if(previous.isPresent()) {
            var r=previous.get();
            if(!campaignId.equals(r.getCampaignId()) || !request.merchId().equals(r.getMerchId()) || request.quantity()!=r.getQuantity() || !Objects.equals(request.note(),r.getNote()))
                throw new AppException("Request ID has already been used for a different reservation.",org.springframework.http.HttpStatus.CONFLICT,"IDEMPOTENCY_CONFLICT");
            return new ReservedOrder(ReservationResponse.from(r),orders.getCustomerOrder(user,r.getOrderId()));
        }
        if(campaign.getState()!=Campaign.State.ACTIVE || !campaign.getDeadline().isAfter(Instant.now()) || !activeOrg(campaign.getOrgId()))
            throw new ValidationException("Campaign is not accepting reservations.");
        Long count=jdbc.queryForObject("SELECT COUNT(*) FROM campaign_reservations WHERE campaign_id=:id",Map.of("id",campaignId),Long.class);
        if(count>=1000) throw new ValidationException("Campaign reservation limit reached.");
        variants.findByCampaignIdAndMerchId(campaignId,request.merchId()).orElseThrow(()->new ValidationException("Invalid campaign variant."));
        InstantOrderRequest orderRequest=new InstantOrderRequest(); orderRequest.setMerchId(request.merchId());
        orderRequest.setQuantity(request.quantity()); orderRequest.setNote(request.note());
        var order=orders.createCampaignOrder(user,orderRequest,campaignId);
        CampaignReservation row=new CampaignReservation(); row.setId(UUID.randomUUID()); row.setCampaignId(campaignId);
        row.setUserId(user); row.setMerchId(request.merchId()); row.setOrderId(order.getId()); row.setQuantity(request.quantity());
        row.setRequestId(request.requestId()); row.setNote(request.note()); row.setCreatedAt(Instant.now()); reservations.saveAndFlush(row);
        return new ReservedOrder(ReservationResponse.from(row),order);
    }
    @Transactional(readOnly=true)
    public Page<ReservationResponse> reservations(UUID user,Pageable page) {
        return reservations.findByUserId(user,page).map(ReservationResponse::from);
    }
    @Transactional(readOnly=true)
    public CampaignResponse publicDetail(UUID id) {
        Campaign c=campaigns.findById(id).filter(row->activeOrg(row.getOrgId())).orElseThrow(()->missing(id)); return detail(c);
    }
    @Transactional(readOnly=true)
    public Page<CampaignResponse> publicList(Pageable page) { return summaries(campaigns.findPublic(page)); }
    @Transactional(readOnly=true)
    public Page<CampaignResponse> organizationList(UUID owner,UUID org,Pageable page) {
        organizations.getOwnOrganizationEntity(owner,org); return summaries(campaigns.findByOrgId(org,page));
    }
    @Transactional
    public CampaignResponse cancel(UUID owner,UUID org,UUID id) {
        organizations.getOwnOrganizationEntity(owner,org);
        Campaign c=lock(id); if(!org.equals(c.getOrgId())) throw missing(id);
        return close(c,owner,true);
    }
    @Transactional
    public CampaignResponse finalizeDue(UUID id) {
        Campaign c=lock(id);
        if(c.getState()==Campaign.State.ACTIVE && c.getDeadline().isAfter(Instant.now())) return detail(c);
        return close(c,null,false);
    }
    private CampaignResponse close(Campaign c,UUID actor,boolean cancel) {
        if(c.getState()!=Campaign.State.ACTIVE) return detail(c);
        // Campaign -> all orders (UUID order) -> all SKU rows (UUID order), shared by concurrent closers.
        var lockedOrders=reservations.orderIds(c.getId()).stream().map(id->orderRepository.findLockedById(id).orElseThrow()).toList();
        var specs=variants.findByCampaignIdOrderById(c.getId());
        var products=merchandise.findAllLockedByIds(specs.stream().map(CampaignVariant::getMerchId).toList());
        long quantity=quantities(List.of(c.getId())).getOrDefault(c.getId(),0L);
        boolean eligible=activeOrg(c.getOrgId()) && products.size()==specs.size()
            && products.stream().allMatch(m->m.getStatus()==MerchItemStatus.PUBLISHED && c.getOrgId().equals(m.getOrgId()));
        c.setClosedQuantity(quantity);
        c.setState(cancel?Campaign.State.CANCELLED:eligible && quantity>=c.getMinimumQuantity()?Campaign.State.SUCCEEDED:Campaign.State.FAILED);
        campaigns.saveAndFlush(c);
        if(c.getState()!=Campaign.State.SUCCEEDED) for(var order:lockedOrders) {
            if(order.getStatus()!=OrderStatus.CANCELLED) orders.cancelCampaignOrder(order.getId(),actor,
                cancel?"Preorder campaign cancelled.":"Preorder campaign did not meet its threshold or is no longer available.");
        }
        return detail(c);
    }
    private Campaign lock(UUID id) { return campaigns.lock(id).orElseThrow(()->missing(id)); }
    private boolean activeOrg(UUID id) { return orgRepository.findById(id).filter(o->o.getStatus()==OrganizationStatus.ACTIVE).isPresent(); }
    private ResourceNotFoundException missing(UUID id) { return new ResourceNotFoundException("Campaign",id.toString()); }
    private CampaignResponse detail(Campaign c) {
        var specs=variants.findByCampaignIdOrderById(c.getId());
        var products=merchandise.findAllById(specs.stream().map(CampaignVariant::getMerchId).toList()).stream().collect(Collectors.toMap(MerchItem::getId,m->m));
        boolean open=c.getState()==Campaign.State.ACTIVE && c.getDeadline().isAfter(Instant.now()) && activeOrg(c.getOrgId());
        var details=specs.stream().map(v->{var m=products.get(v.getMerchId()); boolean available=open && m!=null && m.getStatus()==MerchItemStatus.PUBLISHED && c.getOrgId().equals(m.getOrgId());
            return new CampaignResponse.Variant(v.getMerchId(),v.getLabel(),v.getUnitPrice(),available?m.getStock():0,available && m.getStock()>0);}).toList();
        long quantity=c.getClosedQuantity()!=null?c.getClosedQuantity():quantities(List.of(c.getId())).getOrDefault(c.getId(),0L);
        return CampaignResponse.from(c,quantity,details);
    }
    private Page<CampaignResponse> summaries(Page<Campaign> page) {
        var quantities=quantities(page.getContent().stream().filter(c->c.getClosedQuantity()==null).map(Campaign::getId).toList());
        return page.map(c->CampaignResponse.from(c,c.getClosedQuantity()!=null?c.getClosedQuantity():quantities.getOrDefault(c.getId(),0L),List.of()));
    }
    private Map<UUID,Long> quantities(List<UUID> ids) {
        if(ids.isEmpty()) return Map.of();
        Map<UUID,Long> result=new HashMap<>();
        jdbc.query("""
            SELECT r.campaign_id, SUM(r.quantity) AS quantity FROM campaign_reservations r JOIN orders o ON o.id=r.order_id
            WHERE r.campaign_id IN (:ids) AND o.status<>'CANCELLED' GROUP BY r.campaign_id
            """,Map.of("ids",ids),(org.springframework.jdbc.core.RowCallbackHandler)rs->result.put(rs.getObject("campaign_id",UUID.class),rs.getLong("quantity")));
        return result;
    }
}
