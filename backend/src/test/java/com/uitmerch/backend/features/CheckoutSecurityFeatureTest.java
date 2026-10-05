package com.uitmerch.backend.features;

import com.uitmerch.backend.common.exception.*;
import com.uitmerch.backend.common.model.*;
import com.uitmerch.backend.common.security.SecurityCredentials;
import com.uitmerch.backend.common.service.RateLimiterService;
import com.uitmerch.backend.order.dto.*;
import com.uitmerch.backend.order.repository.OrderRepository;
import com.uitmerch.backend.order.service.OrderService;
import com.uitmerch.backend.order.security.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.mockito.ArgumentCaptor;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@EnabledIfEnvironmentVariable(named="UITMERCH_TEST_DATABASE_URL",matches="jdbc:postgresql:.*")
class CheckoutSecurityFeatureTest extends BackendFeatureTest {
    @Autowired CheckoutSecurityService security;
    @Autowired OrderService orders;
    @Autowired OrderRepository repository;
    @Autowired CheckoutActorRepository actors;
    @Autowired GuestTrackingService tracking;
    @Autowired GuestTrackingRepository trackingCredentials;
    @Autowired RateLimiterService rates;

    private InstantOrderRequest instant(UUID merch,int quantity,UUID id) {
        var r=new InstantOrderRequest();r.setMerchId(merch);r.setQuantity(quantity);r.setRequestId(id);return r;
    }
    private GuestOrderRequest guest(UUID merch,String email,String token) {
        var item=new GuestOrderItemRequest();item.setMerchId(merch);item.setQuantity(1);
        var r=new GuestOrderRequest();r.setRequestId(UUID.randomUUID());r.setItems(List.of(item));r.setGuestName("Guest");
        r.setGuestPhone("0901234567");r.setGuestEmail(email);r.setGuestCheckoutToken(token);return r;
    }
    private String[] verification(String email) {
        reset(transport);
        UUID challenge=security.challenge(email);drain();
        var capture=ArgumentCaptor.forClass(String.class);verify(transport).sendOtp(eq(email),capture.capture());
        return new String[]{challenge.toString(),capture.getValue()};
    }
    @Test void guestVerificationAttemptsCommitAndExpiredOrForeignCredentialsCannotHoldStock() {
        String email=UUID.randomUUID()+"@uit.edu.vn";var verification=verification(email);
        String wrong=verification[1].equals("000000")?"111111":"000000";
        for(int i=0;i<5;i++)assertThat(security.verify(email,UUID.fromString(verification[0]),wrong)).isNull();
        assertThat(actors.findById("guest:"+SecurityCredentials.hash(email)).orElseThrow().getFailedAttempts()).isEqualTo(5);
        assertThat(security.verify(email,UUID.fromString(verification[0]),verification[1])).isNull();
        verification=verification(email);
        String grant=security.verify(email,UUID.fromString(verification[0]),verification[1]);assertThat(grant).hasSize(43);
        var product=product(organization(),20);
        assertThatThrownBy(()->orders.createGuestOrder(guest(product.getId(),"foreign@uit.edu.vn",grant)))
            .isInstanceOf(AuthenticationException.class);
        var placed=orders.createGuestOrder(guest(product.getId(),email,grant)).getFirst();
        assertThat(placed.getUserId()).isNull();assertThat(merch.findById(product.getId()).orElseThrow().getStock()).isEqualTo(19);
        tx.executeWithoutResult(s->actors.locked("guest:"+SecurityCredentials.hash(email)).orElseThrow().setVerifiedUntil(Instant.now().minusSeconds(1)));
        assertThatThrownBy(()->orders.createGuestOrder(guest(product.getId(),email,grant))).isInstanceOf(AuthenticationException.class);
        assertThat(merch.findById(product.getId()).orElseThrow().getStock()).isEqualTo(19);
    }
    @Test void concurrentIdempotencyReplaysOneCheckoutAndConflictingPayloadIsRejected() throws Exception {
        var user=user(UserRole.CUSTOMER);var product=product(organization(),20);UUID key=UUID.randomUUID();
        assertThat(parallel(5,()->orders.createInstantOrder(user.getId(),instant(product.getId(),2,key)))).containsOnly(true);
        assertThat(merch.findById(product.getId()).orElseThrow().getStock()).isEqualTo(18);
        var original=orders.createInstantOrder(user.getId(),instant(product.getId(),2,key));
        assertThat(repository.findByUserId(user.getId(),org.springframework.data.domain.PageRequest.of(0,20)).getTotalElements()).isEqualTo(1);
        assertThat(orders.createInstantOrder(user.getId(),instant(product.getId(),2,key)).getId()).isEqualTo(original.getId());
        assertThatThrownBy(()->orders.createInstantOrder(user.getId(),instant(product.getId(),3,key))).isInstanceOf(AppException.class);
    }
    @Test void quantityAggregationAndConcurrentPendingQuotaCannotBeBypassed() throws Exception {
        var user=user(UserRole.CUSTOMER);var product=product(organization(),100);
        var request=guest(product.getId(),user.getEmail(),null);
        request.getItems().getFirst().setQuantity(6);
        var duplicate=new GuestOrderItemRequest();duplicate.setMerchId(product.getId());duplicate.setQuantity(6);
        request.setItems(List.of(request.getItems().getFirst(),duplicate));
        assertThatThrownBy(()->orders.createPublicOrder(user.getId(),request)).isInstanceOf(ValidationException.class);
        assertThat(parallel(8,()->orders.createInstantOrder(user.getId(),instant(product.getId(),1,UUID.randomUUID()))))
            .filteredOn(Boolean::booleanValue).hasSize(3);
        assertThat(merch.findById(product.getId()).orElseThrow().getStock()).isEqualTo(97);
    }
    @Test void expiryRestoresOnceAndPreventsLateConfirmation() throws Exception {
        var user=user(UserRole.CUSTOMER);var org=organization();var product=product(org,10);
        var result=orders.createInstantOrder(user.getId(),instant(product.getId(),2,UUID.randomUUID()));
        var pending=repository.findById(result.getId()).orElseThrow();
        assertThat(pending.getPendingExpiresAt()).isBetween(Instant.now().plusSeconds(47*3600),Instant.now().plusSeconds(49*3600));
        pending.setPendingExpiresAt(Instant.now().minusSeconds(1));repository.saveAndFlush(pending);
        assertThatThrownBy(()->orders.updateOrderStatus(org.getOwnerId(),org.getId(),result.getId(),OrderStatus.CONFIRMED)).isInstanceOf(ValidationException.class);
        assertThat(parallel(4,()->orders.expirePendingOrder(result.getId()))).containsOnly(true);
        assertThat(repository.findById(result.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(merch.findById(product.getId()).orElseThrow().getStock()).isEqualTo(10);
    }
    @Test void guestTrackingRequiresEmailPossessionAndRedactsPrivateFields() throws Exception {
        String email=UUID.randomUUID()+"@uit.edu.vn";var verification=verification(email);
        var token=security.verify(email,UUID.fromString(verification[0]),verification[1]);
        var result=orders.createGuestOrder(guest(product(organization(),10).getId(),email,token)).getFirst();
        reset(transport);tracking.request(result.getId(),email);drain();
        var message=ArgumentCaptor.forClass(String.class);
        verify(transport).sendAnnouncement(eq(email),eq("Mã tra cứu đơn hàng"),message.capture());
        var matcher=java.util.regex.Pattern.compile("[A-Za-z0-9_-]{43}").matcher(message.getValue());assertThat(matcher.find()).isTrue();
        String credential=matcher.group();
        assertThatThrownBy(()->tracking.read(result.getId(),null)).isInstanceOf(ResourceNotFoundException.class);
        var read=tracking.read(result.getId(),credential);assertThat(read.getGuestEmail()).isNull();assertThat(read.getGuestPhone()).isNull();assertThat(read.getNote()).isNull();
        mvc.perform(get("/api/v1/public/orders/"+result.getId()).param("email",email)).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/public/orders/"+result.getId()+"/tracking").header("X-Guest-Tracking",credential))
            .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"));
        var row=trackingCredentials.findById(result.getId()).orElseThrow();row.setExpiresAt(Instant.now().minusSeconds(1));trackingCredentials.save(row);
        assertThatThrownBy(()->tracking.read(result.getId(),credential)).isInstanceOf(ResourceNotFoundException.class);
    }
    @Test void sharedRateLimitSerializesConcurrentRequestsAndSurvivesFacadeReplacement() throws Exception {
        String key="security-test:"+UUID.randomUUID();
        assertThat(parallel(8,()->{if(!rates.isAllowed(key,3,Duration.ofMinutes(5)))throw new ValidationException("Limited");}))
            .filteredOn(Boolean::booleanValue).hasSize(3);
        assertThat(rates.isAllowed(key,3,Duration.ofMinutes(5))).isFalse();
    }
    @Test void authenticatedAndGuestCheckoutsShareTheSameBuyerQuota() {
        var customer=user(UserRole.CUSTOMER);var product=product(organization(),10);
        var verification=verification(customer.getEmail());
        String grant=security.verify(customer.getEmail(),UUID.fromString(verification[0]),verification[1]);
        orders.createGuestOrder(guest(product.getId(),customer.getEmail(),grant));
        orders.createInstantOrder(customer.getId(),instant(product.getId(),1,UUID.randomUUID()));
        orders.createInstantOrder(customer.getId(),instant(product.getId(),1,UUID.randomUUID()));
        assertThatThrownBy(()->orders.createGuestOrder(guest(product.getId(),customer.getEmail(),grant))).isInstanceOf(ValidationException.class);
        assertThatThrownBy(()->orders.createInstantOrder(customer.getId(),instant(product.getId(),1,UUID.randomUUID()))).isInstanceOf(ValidationException.class);
        assertThat(merch.findById(product.getId()).orElseThrow().getStock()).isEqualTo(7);
    }
    @Test void notificationStreamsRejectTokensInQueryStrings() throws Exception {
        var customer=user(UserRole.CUSTOMER);
        mvc.perform(get("/api/v1/customer/notifications/stream").param("token",token(customer)))
            .andExpect(status().isUnauthorized());
    }
    @Test void inactiveOrganizationsAndDisabledOrganizersCannotAcceptStockReservations() {
        var customer=user(UserRole.CUSTOMER);var org=organization();var product=product(org,10);
        org.setStatus(OrganizationStatus.INACTIVE);organizations.saveAndFlush(org);
        assertThatThrownBy(()->orders.createInstantOrder(customer.getId(),instant(product.getId(),1,UUID.randomUUID()))).isInstanceOf(ValidationException.class);
        org.setStatus(OrganizationStatus.ACTIVE);organizations.saveAndFlush(org);
        var owner=users.findById(org.getOwnerId()).orElseThrow();owner.setActive(false);users.saveAndFlush(owner);
        assertThatThrownBy(()->orders.createInstantOrder(customer.getId(),instant(product.getId(),1,UUID.randomUUID()))).isInstanceOf(ValidationException.class);
        assertThat(merch.findById(product.getId()).orElseThrow().getStock()).isEqualTo(10);
    }
}
