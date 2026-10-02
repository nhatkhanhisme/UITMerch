package com.uitmerch.backend.features;
import com.uitmerch.backend.following.*;
import com.uitmerch.backend.common.model.*;
import com.uitmerch.backend.merch.dto.UpdateMerchRequest;
import com.uitmerch.backend.event.dto.*;
import com.uitmerch.backend.event.service.EventService;
import com.uitmerch.backend.notification.repository.NotificationRepository;
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
class FollowingFeatureTest extends BackendFeatureTest {
    @Autowired FollowService follows;
    @Autowired EventService events;
    @Autowired NotificationRepository notifications;
    private long alerts(UUID user, NotificationType type) {
        return notifications.findByUserIdOrderByCreatedAtDesc(user, PageRequest.of(0, 100)).stream().filter(n -> n.getType() == type).count();
    }
    private void publish(com.uitmerch.backend.organization.entity.Organization org, UUID id) {
        UpdateMerchRequest p = new UpdateMerchRequest(); p.setStatus(MerchItemStatus.PUBLISHED);
        merchandise.updateMerch(org.getOwnerId(), org.getId(), id, p);
    }
    private com.uitmerch.backend.merch.entity.MerchItem draft(com.uitmerch.backend.organization.entity.Organization org) {
        var item = product(org, 2); item.setStatus(MerchItemStatus.DRAFT); return merch.save(item);
    }
    @Test void publicationAlertsHonorPreferencesAndEmailIsOptIn() {
        var org = organization(); var a = user(UserRole.CUSTOMER); var b = user(UserRole.CUSTOMER);
        follows.follow(a.getId(), org.getId(), null);
        FollowPreferences preferences = new FollowPreferences(); preferences.setNotifyMerch(false); preferences.setEmailEnabled(true);
        follows.follow(b.getId(), org.getId(), preferences);
        var product = draft(org); publish(org, product.getId()); drain();
        assertThat(alerts(a.getId(), NotificationType.MERCH_PUBLISHED)).isEqualTo(1);
        assertThat(alerts(b.getId(), NotificationType.MERCH_PUBLISHED)).isZero();
        verify(transport, never()).sendAnnouncement(anyString(), anyString(), anyString());
        CreateEventRequest event = new CreateEventRequest(); event.setTitle("UIT Event"); event.setStatus(EventStatus.PUBLISHED);
        events.createEvent(org.getOwnerId(), org.getId(), event); drain();
        assertThat(alerts(a.getId(), NotificationType.EVENT_PUBLISHED)).isEqualTo(1);
        assertThat(alerts(b.getId(), NotificationType.EVENT_PUBLISHED)).isEqualTo(1);
        verify(transport).sendAnnouncement(eq(b.getEmail()), anyString(), anyString());
        verify(transport, never()).sendAnnouncement(eq(a.getEmail()), anyString(), anyString());
    }
    @Test void concurrentPublishAndRepublishDoNotSpam() throws Exception {
        var org = organization(); var a = user(UserRole.CUSTOMER); follows.follow(a.getId(), org.getId(), null);
        var product = draft(org);
        assertThat(parallel(3, () -> publish(org, product.getId()))).containsOnly(true); drain();
        UpdateMerchRequest draft = new UpdateMerchRequest(); draft.setStatus(MerchItemStatus.DRAFT);
        merchandise.updateMerch(org.getOwnerId(), org.getId(), product.getId(), draft); publish(org, product.getId()); drain();
        assertThat(alerts(a.getId(), NotificationType.MERCH_PUBLISHED)).isEqualTo(1);
        CreateEventRequest request = new CreateEventRequest(); request.setTitle("Event"); request.setStatus(EventStatus.DRAFT);
        var event = events.createEvent(org.getOwnerId(), org.getId(), request);
        UpdateEventRequest p = new UpdateEventRequest(); p.setStatus(EventStatus.PUBLISHED);
        assertThat(parallel(3, () -> events.updateEvent(org.getOwnerId(), org.getId(), event.getId(), p))).containsOnly(true); drain();
        assertThat(alerts(a.getId(), NotificationType.EVENT_PUBLISHED)).isEqualTo(1);
    }
    @Test void unfollowPreferencesAndLateFollowSuppressPendingAnnouncements() {
        var org = organization(); var a = user(UserRole.CUSTOMER); follows.follow(a.getId(), org.getId(), null);
        var product = draft(org); publish(org, product.getId()); follows.unfollow(a.getId(), org.getId());
        var late = user(UserRole.CUSTOMER); follows.follow(late.getId(), org.getId(), null); drain();
        assertThat(alerts(a.getId(), NotificationType.MERCH_PUBLISHED)).isZero(); assertThat(alerts(late.getId(), NotificationType.MERCH_PUBLISHED)).isZero();
        var another = draft(org); publish(org, another.getId());
        FollowPreferences off = new FollowPreferences(); off.setNotifyMerch(false); follows.preferences(late.getId(), org.getId(), off); drain();
        assertThat(alerts(late.getId(), NotificationType.MERCH_PUBLISHED)).isZero();
        assertThat(follows.list(late.getId(), PageRequest.of(0, 20)).getContent().getFirst().isNotifyEvents()).isTrue();
    }
    @Test void rollbackAndInactiveOrganizationSuppressDelivery() {
        var org = organization(); var a = user(UserRole.CUSTOMER); follows.follow(a.getId(), org.getId(), null);
        var product = draft(org);
        assertThatThrownBy(() -> tx.executeWithoutResult(s -> { publish(org, product.getId()); throw new IllegalStateException("rollback"); }))
            .isInstanceOf(IllegalStateException.class);
        assertThat(jobs.count()).isZero();
        publish(org, product.getId()); org.setStatus(OrganizationStatus.INACTIVE); organizations.save(org); drain();
        assertThat(alerts(a.getId(), NotificationType.MERCH_PUBLISHED)).isZero();
    }
    @Test void concurrentFollowIsUniqueAndApiListsOnlyCurrentUsersFollows() throws Exception {
        var org = organization(); var a = user(UserRole.CUSTOMER); var b = user(UserRole.CUSTOMER);
        assertThat(parallel(4, () -> follows.follow(a.getId(), org.getId(), null))).containsOnly(true);
        assertThat(follows.list(a.getId(), PageRequest.of(0, 20)).getTotalElements()).isEqualTo(1);
        mvc.perform(get("/api/v1/customer/following").header("Authorization", "Bearer " + token(b)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.content.length()").value(0));
        mvc.perform(post("/api/v1/customer/following/" + org.getId())).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/customer/following/" + org.getId()).header("Authorization", "Bearer " + token(user(UserRole.ORGANIZER))))
            .andExpect(status().isForbidden());
        mvc.perform(patch("/api/v1/customer/following/" + org.getId()).header("Authorization", "Bearer " + token(a))
            .contentType("application/json").content("{\"emailEnabled\":true}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.emailEnabled").value(true)).andExpect(jsonPath("$.data.notifyMerch").value(true));
        mvc.perform(delete("/api/v1/customer/following/" + org.getId()).header("Authorization", "Bearer " + token(a))).andExpect(status().isOk());
    }
}
