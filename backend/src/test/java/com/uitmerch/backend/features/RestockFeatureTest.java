package com.uitmerch.backend.features;

import com.uitmerch.backend.common.model.*;
import com.uitmerch.backend.merch.dto.UpdateMerchRequest;
import com.uitmerch.backend.restock.*;
import com.uitmerch.backend.notification.repository.NotificationRepository;
import com.uitmerch.backend.notification.announcement.AnnouncementRepository;
import com.uitmerch.backend.order.service.OrderService;
import com.uitmerch.backend.order.dto.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@EnabledIfEnvironmentVariable(named = "UITMERCH_TEST_DATABASE_URL", matches = "jdbc:postgresql:.*")
class RestockFeatureTest extends BackendFeatureTest {
    @Autowired RestockService restock;
    @Autowired RestockRepository subscriptions;
    @Autowired NotificationRepository notifications;
    @Autowired AnnouncementRepository announcements;
    @Autowired OrderService orders;
    private void stock(com.uitmerch.backend.organization.entity.Organization org, UUID id, int quantity) {
        UpdateMerchRequest request = new UpdateMerchRequest(); request.setStock(quantity);
        merchandise.updateMerch(org.getOwnerId(), org.getId(), id, request);
    }
    private long alerts(UUID userId) {
        return notifications.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 200)).stream()
            .filter(n -> n.getType() == NotificationType.MERCH_RESTOCKED).count();
    }
    @Test void subscribeApiIsOwnedIdempotentAndRoleChecked() throws Exception {
        var org = organization(); var item = product(org, 0); var customer = user(UserRole.CUSTOMER);
        String jwt = token(customer);
        for (int i = 0; i < 2; i++) mvc.perform(post("/api/v1/customer/restock-subscriptions")
            .header("Authorization", "Bearer " + jwt).contentType("application/json")
            .content(json.writeValueAsString(Map.of("merchId", item.getId())))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/customer/restock-subscriptions").header("Authorization", "Bearer " + jwt))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.content.length()").value(1));
        mvc.perform(post("/api/v1/customer/restock-subscriptions").contentType("application/json")
            .content(json.writeValueAsString(Map.of("merchId", item.getId())))).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/customer/restock-subscriptions").header("Authorization", "Bearer " + token(user(UserRole.ORGANIZER)))
            .contentType("application/json").content(json.writeValueAsString(Map.of("merchId", item.getId())))).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/customer/restock-subscriptions").header("Authorization", "Bearer " + jwt)
            .contentType("application/json").content("{}" )).andExpect(status().isBadRequest());
    }
    @Test void oneAlertPerStockCycleAndEmailPreferenceIsRespected() {
        var org = organization(); var item = product(org, 0);
        var a = user(UserRole.CUSTOMER); var b = user(UserRole.CUSTOMER);
        restock.subscribe(a.getId(), item.getId(), true); restock.subscribe(b.getId(), item.getId(), false);
        stock(org, item.getId(), 2); drain();
        stock(org, item.getId(), 3); drain();
        assertThat(alerts(a.getId())).isEqualTo(1); assertThat(alerts(b.getId())).isEqualTo(1);
        verify(transport).sendAnnouncement(eq(a.getEmail()), anyString(), anyString());
        verify(transport, never()).sendAnnouncement(eq(b.getEmail()), anyString(), anyString());
        stock(org, item.getId(), 0); stock(org, item.getId(), 4); drain();
        assertThat(alerts(a.getId())).isEqualTo(2);
        assertThat(merch.findById(item.getId()).orElseThrow().getRestockCycle()).isEqualTo(2);
    }
    @Test void unsubscribeLateSubscribeAndArchivalSuppressQueuedAlerts() {
        var org = organization(); var item = product(org, 0); var a = user(UserRole.CUSTOMER);
        restock.subscribe(a.getId(), item.getId(), true); stock(org, item.getId(), 1);
        restock.unsubscribe(a.getId(), item.getId());
        var late = user(UserRole.CUSTOMER); restock.subscribe(late.getId(), item.getId(), true); drain();
        assertThat(alerts(a.getId())).isZero(); assertThat(alerts(late.getId())).isZero();
        stock(org, item.getId(), 0); stock(org, item.getId(), 2);
        merchandise.deleteMerch(org.getOwnerId(), org.getId(), item.getId()); drain();
        assertThat(alerts(late.getId())).isZero();
    }
    @Test void cancellationRestoresStockAndNotifiesOnceDespiteConcurrentCancel() throws Exception {
        var org = organization(); var item = product(org, 1); var a = user(UserRole.CUSTOMER);
        restock.subscribe(a.getId(), item.getId(), false);
        InstantOrderRequest purchase = new InstantOrderRequest(); purchase.setRequestId(UUID.randomUUID()); purchase.setMerchId(item.getId()); purchase.setQuantity(1);
        var order = orders.createInstantOrder(a.getId(), purchase);
        CancelOrderRequest cancel = new CancelOrderRequest(); cancel.setCancelReason("Changed mind");
        assertThat(parallel(2, () -> orders.cancelCustomerOrder(a.getId(), order.getId(), cancel))).containsExactlyInAnyOrder(true, false);
        drain(); assertThat(alerts(a.getId())).isEqualTo(1);
        assertThat(merch.findById(item.getId()).orElseThrow().getStock()).isEqualTo(1);
    }
    @Test void concurrentSubscriptionsAndRestockingGenerateOneCycle() throws Exception {
        var org = organization(); var item = product(org, 0); var a = user(UserRole.CUSTOMER);
        assertThat(parallel(4, () -> restock.subscribe(a.getId(), item.getId(), false))).containsOnly(true);
        assertThat(subscriptions.findByUserIdAndEnabledTrue(a.getId(), PageRequest.of(0, 20)).getTotalElements()).isEqualTo(1);
        assertThat(parallel(4, () -> stock(org, item.getId(), 1))).containsOnly(true);
        drain(); assertThat(alerts(a.getId())).isEqualTo(1);
    }
    @Test void rollbackDiscardsStockChangeAndAllDeliveryRecords() {
        var org = organization(); var item = product(org, 0); var a = user(UserRole.CUSTOMER);
        restock.subscribe(a.getId(), item.getId(), true); long count = announcements.count();
        assertThatThrownBy(() -> tx.executeWithoutResult(s -> { stock(org, item.getId(), 1); throw new IllegalStateException("rollback"); }))
            .isInstanceOf(IllegalStateException.class);
        assertThat(announcements.count()).isEqualTo(count); assertThat(jobs.count()).isZero();
        assertThat(merch.findById(item.getId()).orElseThrow().getStock()).isZero();
    }
    @Test void fanoutContinuesAcrossBatchesWithoutDuplicateNotifications() {
        var org = organization(); var item = product(org, 0); List<UUID> audience = new ArrayList<>();
        for (int i = 0; i < 51; i++) {
            var customer = user(UserRole.CUSTOMER); audience.add(customer.getId());
            restock.subscribe(customer.getId(), item.getId(), false);
        }
        stock(org, item.getId(), 1); drain();
        for (var id : audience) assertThat(alerts(id)).isEqualTo(1);
        var event = announcements.findAll().stream().filter(e -> item.getId().equals(e.getMerchId())).findFirst().orElseThrow();
        assertThat(event.isFinished()).isTrue();
        tx.executeWithoutResult(s -> jobs.save(newJob(event.getId()))); drain();
        for (var id : audience) assertThat(alerts(id)).isEqualTo(1);
    }
    private com.uitmerch.backend.common.delivery.BackgroundJob newJob(UUID event) {
        var job = new com.uitmerch.backend.common.delivery.BackgroundJob(); job.setKind("ANNOUNCEMENT"); job.setPayload(event.toString()); return job;
    }
}
