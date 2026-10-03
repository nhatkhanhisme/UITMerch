package com.uitmerch.backend.features;

import com.uitmerch.backend.campaign.*;
import com.uitmerch.backend.common.exception.*;
import com.uitmerch.backend.common.model.*;
import com.uitmerch.backend.order.dto.*;
import com.uitmerch.backend.order.service.OrderService;
import com.uitmerch.backend.order.repository.OrderRepository;
import com.uitmerch.backend.order.history.OrderHistoryService;
import com.uitmerch.backend.organization.entity.Organization;
import com.uitmerch.backend.merch.entity.MerchItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.domain.PageRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@EnabledIfEnvironmentVariable(named="UITMERCH_TEST_DATABASE_URL",matches="jdbc:postgresql:.*")
class CampaignFeatureTest extends BackendFeatureTest {
    @Autowired CampaignService campaigns;
    @Autowired CampaignRepository campaignRepository;
    @Autowired CampaignReservationRepository reservations;
    @Autowired OrderService orders;
    @Autowired OrderRepository orderRepository;
    @Autowired OrderHistoryService history;
    @Autowired JdbcTemplate jdbc;
    private CampaignResponse campaign(Organization org,MerchItem item,int minimum) {
        return campaigns.create(org.getOwnerId(),org.getId(),new CampaignRequests.Create("UIT Preorder","Size variants",minimum,
            Instant.now().plusSeconds(3600),List.of(new CampaignRequests.Variant(item.getId(),"Blue / M"))));
    }
    private CampaignRequests.Reserve request(MerchItem item,int quantity) { return new CampaignRequests.Reserve(item.getId(),quantity,UUID.randomUUID(),null); }
    private void due(UUID id) { jdbc.update("UPDATE preorder_campaigns SET deadline=? WHERE id=?",java.sql.Timestamp.from(Instant.now().minusSeconds(5)),id); }
    @Test
    @org.springframework.transaction.annotation.Transactional
    void publicCampaignPaginationUsesDatabaseEnumAndExcludesInactiveOrganizations() throws Exception {
        long existing = jdbc.queryForObject("""
            SELECT COUNT(*) FROM preorder_campaigns c JOIN organizations o ON o.id=c.org_id
            WHERE o.status='ACTIVE'
            """, Long.class);
        var active = organization();
        var first = campaign(active, product(active,10), 2);
        var second = campaign(active, product(active,10), 2);
        var inactive = organization();
        campaign(inactive, product(inactive,10), 2);
        inactive.setStatus(OrganizationStatus.INACTIVE);
        organizations.saveAndFlush(inactive);

        // A full one-item page forces both the content query and its count query.
        mvc.perform(get("/api/v1/public/campaigns").param("size","1").param("sort","createdAt,desc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalElements").value(existing+2))
            .andExpect(jsonPath("$.data.content.length()").value(1))
            .andExpect(jsonPath("$.data.content[0].id").value(second.id().toString()));
        mvc.perform(get("/api/v1/public/campaigns").param("page","1").param("size","1").param("sort","createdAt,desc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalElements").value(existing+2))
            .andExpect(jsonPath("$.data.content[0].id").value(first.id().toString()));
    }
    @Test void concurrentReplayCreatesOneOrderAndUsesCampaignPriceSnapshot() throws Exception {
        var org=organization(); var item=product(org,10); var c=campaign(org,item,2); var user=user(UserRole.CUSTOMER);
        item.setPrice(new BigDecimal("200000")); merch.saveAndFlush(item);
        var request=request(item,2);
        assertThat(parallel(4,()->campaigns.reserve(user.getId(),c.id(),request))).containsOnly(true);
        var row=reservations.findByUserIdAndRequestId(user.getId(),request.requestId()).orElseThrow();
        assertThat(merch.findById(item.getId()).orElseThrow().getStock()).isEqualTo(8);
        var replay=campaigns.reserve(user.getId(),c.id(),request);
        assertThat(replay.order().getId()).isEqualTo(row.getOrderId());
        assertThat(replay.order().getTotalAmount()).isEqualByComparingTo("200000");
        assertThat(replay.order().getItems().getFirst().getUnitPrice()).isEqualByComparingTo("100000");
        assertThatThrownBy(()->campaigns.reserve(user.getId(),c.id(),new CampaignRequests.Reserve(item.getId(),1,request.requestId(),null)))
            .isInstanceOf(AppException.class).hasMessageContaining("different reservation");
        assertThat(campaigns.publicDetail(c.id()).reservedQuantity()).isEqualTo(2);
    }
    @Test void normalCheckoutCannotBypassCampaignAndOrdersCannotProgressEarly() {
        var org=organization(); var item=product(org,10); var c=campaign(org,item,2); var user=user(UserRole.CUSTOMER);
        InstantOrderRequest instant=new InstantOrderRequest(); instant.setMerchId(item.getId()); instant.setQuantity(1);
        assertThatThrownBy(()->orders.createInstantOrder(user.getId(),instant)).isInstanceOf(ValidationException.class);
        GuestOrderRequest guest=new GuestOrderRequest(); GuestOrderItemRequest line=new GuestOrderItemRequest(); line.setMerchId(item.getId()); line.setQuantity(1); guest.setItems(List.of(line));
        assertThatThrownBy(()->orders.createPublicOrder(null,guest)).isInstanceOf(ValidationException.class);
        var cart=new com.uitmerch.backend.cart.entity.Cart();
        var cartLine=new com.uitmerch.backend.cart.entity.CartItem(); cartLine.setMerchId(item.getId()); cartLine.setQuantity(1);
        assertThatThrownBy(()->orders.createOrdersFromCart(user.getId(),cart,List.of(cartLine),null,null,null,null)).isInstanceOf(ValidationException.class);
        var reserved=campaigns.reserve(user.getId(),c.id(),request(item,2));
        assertThatThrownBy(()->orders.updateOrderStatus(org.getOwnerId(),org.getId(),reserved.order().getId(),OrderStatus.CONFIRMED))
            .isInstanceOf(ValidationException.class).hasMessageContaining("must succeed");
        assertThat(orderRepository.findById(reserved.order().getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.PENDING);
    }
    @Test void successAtDeadlineKeepsReservationsAndAllowsFulfillment() throws Exception {
        var org=organization(); var item=product(org,10); var c=campaign(org,item,2); var user=user(UserRole.CUSTOMER); var request=request(item,2);
        var reserved=campaigns.reserve(user.getId(),c.id(),request); due(c.id());
        assertThat(parallel(4,()->campaigns.finalizeDue(c.id()))).containsOnly(true);
        assertThat(campaigns.publicDetail(c.id()).state()).isEqualTo(Campaign.State.SUCCEEDED);
        assertThat(merch.findById(item.getId()).orElseThrow().getStock()).isEqualTo(8);
        assertThat(campaigns.reserve(user.getId(),c.id(),request).order().getId()).isEqualTo(reserved.order().getId());
        orders.updateOrderStatus(org.getOwnerId(),org.getId(),reserved.order().getId(),OrderStatus.CONFIRMED);
        assertThatThrownBy(()->campaigns.reserve(user.getId(),c.id(),request(item,1))).isInstanceOf(ValidationException.class);
    }
    @Test void failedCampaignReleasesExactlyOnceAndAuditsSystemCancellation() throws Exception {
        var org=organization(); var item=product(org,10); var c=campaign(org,item,5); var user=user(UserRole.CUSTOMER);
        var order=campaigns.reserve(user.getId(),c.id(),request(item,2)).order(); due(c.id());
        assertThat(parallel(4,()->campaigns.finalizeDue(c.id()))).containsOnly(true);
        assertThat(campaigns.publicDetail(c.id()).state()).isEqualTo(Campaign.State.FAILED);
        assertThat(merch.findById(item.getId()).orElseThrow().getStock()).isEqualTo(10);
        var cancelled=orderRepository.findById(order.getId()).orElseThrow();
        assertThat(cancelled.getStatus()).isEqualTo(OrderStatus.CANCELLED); assertThat(cancelled.getCancelledBy()).isEqualTo("campaign");
        var records=history.forCustomer(user.getId(),order.getId(),PageRequest.of(0,20));
        assertThat(records.getContent()).hasSize(1); assertThat(records.getContent().getFirst().getSource()).isEqualTo("CAMPAIGN");
        assertThat(records.getContent().getFirst().getActorId()).isNull();
        campaigns.cancel(org.getOwnerId(),org.getId(),c.id());
        assertThat(merch.findById(item.getId()).orElseThrow().getStock()).isEqualTo(10);
    }
    @Test void customerCancellationRacingClosureDoesNotRestoreTwice() throws Exception {
        var org=organization(); var item=product(org,10); var c=campaign(org,item,5); var user=user(UserRole.CUSTOMER);
        var order=campaigns.reserve(user.getId(),c.id(),request(item,3)).order(); due(c.id()); var index=new AtomicInteger();
        parallel(4,()->{ if(index.getAndIncrement()%2==0) orders.cancelCustomerOrder(user.getId(),order.getId(),new CancelOrderRequest()); else campaigns.finalizeDue(c.id()); });
        assertThat(merch.findById(item.getId()).orElseThrow().getStock()).isEqualTo(10);
        assertThat(campaigns.publicDetail(c.id()).state()).isEqualTo(Campaign.State.FAILED);
        assertThat(history.forCustomer(user.getId(),order.getId(),PageRequest.of(0,20)).getTotalElements()).isEqualTo(1);
    }
    @Test void reservationsRollbackTogetherWithInventoryAndRejectExpiredCampaigns() {
        var org=organization(); var item=product(org,10); var c=campaign(org,item,2); var user=user(UserRole.CUSTOMER); var request=request(item,2);
        assertThatThrownBy(()->tx.executeWithoutResult(s->{campaigns.reserve(user.getId(),c.id(),request);throw new IllegalStateException("rollback");})).isInstanceOf(IllegalStateException.class);
        assertThat(reservations.findByUserIdAndRequestId(user.getId(),request.requestId())).isEmpty();
        assertThat(merch.findById(item.getId()).orElseThrow().getStock()).isEqualTo(10); assertThat(jobs.count()).isZero();
        due(c.id()); assertThatThrownBy(()->campaigns.reserve(user.getId(),c.id(),request)).isInstanceOf(ValidationException.class);
    }
    @Test void unavailableOrganizationsFailAndReleaseReservations() {
        var org=organization(); var item=product(org,10); var c=campaign(org,item,2); var user=user(UserRole.CUSTOMER);
        campaigns.reserve(user.getId(),c.id(),request(item,2)); org.setStatus(OrganizationStatus.INACTIVE); organizations.saveAndFlush(org); due(c.id());
        assertThat(campaigns.finalizeDue(c.id()).state()).isEqualTo(Campaign.State.FAILED);
        assertThat(merch.findById(item.getId()).orElseThrow().getStock()).isEqualTo(10);
        assertThatThrownBy(()->campaigns.publicDetail(c.id())).isInstanceOf(ResourceNotFoundException.class);
    }
    @Test void concurrentReservationsCannotOversell() throws Exception {
        var org=organization(); var item=product(org,10); var c=campaign(org,item,5); var user=user(UserRole.CUSTOMER);
        var results=parallel(8,()->campaigns.reserve(user.getId(),c.id(),request(item,2)));
        assertThat(results.stream().filter(Boolean::booleanValue).count()).isEqualTo(5);
        assertThat(merch.findById(item.getId()).orElseThrow().getStock()).isZero();
        assertThat(campaigns.publicDetail(c.id()).reservedQuantity()).isEqualTo(10);
    }
    @Test void sameRequestIdAcrossCampaignsIsUniqueAndLosingOrderRollsBack() throws Exception {
        var org=organization(); var a=product(org,10); var b=product(org,10); var ca=campaign(org,a,2); var cb=campaign(org,b,2);
        var user=user(UserRole.CUSTOMER); var id=UUID.randomUUID(); var start=new CountDownLatch(1);
        try(var executor=Executors.newFixedThreadPool(2)) {
            var futures=new ArrayList<Future<Boolean>>();
            for(var pair:List.of(Map.entry(ca,a),Map.entry(cb,b))) futures.add(executor.submit(()->{
                start.await(); try {campaigns.reserve(user.getId(),pair.getKey().id(),new CampaignRequests.Reserve(pair.getValue().getId(),2,id,null));return true;}
                catch(org.springframework.dao.DataIntegrityViolationException | AppException expected){return false;}
            }));
            start.countDown(); int winners=0; for(var future:futures) if(future.get(20,TimeUnit.SECONDS))winners++;
            assertThat(winners).isEqualTo(1);
        }
        assertThat(merch.findById(a.getId()).orElseThrow().getStock()+merch.findById(b.getId()).orElseThrow().getStock()).isEqualTo(18);
        assertThat(campaigns.reservations(user.getId(),PageRequest.of(0,20)).getTotalElements()).isEqualTo(1);
    }
    @Test void variantsRequireOwnershipAndSchedulerClosesDueCampaigns() {
        var org=organization(); var item=product(org,10); var foreign=product(organization(),10);
        assertThatThrownBy(()->campaign(org,foreign,2)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(()->campaign(org,item,11)).isInstanceOf(ValidationException.class);
        var c=campaign(org,item,2);
        assertThatThrownBy(()->campaign(org,item,2)).isInstanceOf(ValidationException.class).hasMessageContaining("active campaign");
        due(c.id()); new CampaignScheduler(campaignRepository,campaigns).closeDueCampaigns();
        assertThat(campaigns.publicDetail(c.id()).state()).isEqualTo(Campaign.State.FAILED);
    }
    @Test void apiEnforcesRolesOwnershipAndReturnsTypedPublicCampaign() throws Exception {
        var org=organization(); var item=product(org,10); var c=campaign(org,item,2); var customer=user(UserRole.CUSTOMER);
        String cancel="/api/v1/organizations/"+org.getId()+"/campaigns/"+c.id()+"/cancel";
        mvc.perform(get("/api/v1/public/campaigns/"+c.id())).andExpect(status().isOk()).andExpect(jsonPath("$.data.variants[0].label").value("Blue / M"));
        mvc.perform(post(cancel)).andExpect(status().isUnauthorized());
        mvc.perform(post(cancel).header("Authorization","Bearer "+token(customer))).andExpect(status().isForbidden());
        mvc.perform(post(cancel).header("Authorization","Bearer "+token(user(UserRole.ORGANIZER)))).andExpect(status().isNotFound());
        var request=request(item,2);
        mvc.perform(post("/api/v1/customer/campaigns/"+c.id()+"/reservations").header("Authorization","Bearer "+token(customer))
            .contentType("application/json").content(json.writeValueAsString(request))).andExpect(status().isOk()).andExpect(jsonPath("$.data.order.status").value("PENDING"));
        mvc.perform(get("/api/v1/customer/campaign-reservations").header("Authorization","Bearer "+token(user(UserRole.CUSTOMER))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.content.length()").value(0));
        mvc.perform(post(cancel).header("Authorization","Bearer "+token(users.findById(org.getOwnerId()).orElseThrow())))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.state").value("CANCELLED"));
    }
}
