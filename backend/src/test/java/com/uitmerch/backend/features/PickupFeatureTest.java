package com.uitmerch.backend.features;

import com.uitmerch.backend.common.model.*;
import com.uitmerch.backend.common.exception.*;
import com.uitmerch.backend.pickup.*;
import com.uitmerch.backend.order.dto.*;
import com.uitmerch.backend.order.repository.*;
import com.uitmerch.backend.order.service.OrderService;
import com.uitmerch.backend.order.history.OrderHistoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@EnabledIfEnvironmentVariable(named = "UITMERCH_TEST_DATABASE_URL", matches = "jdbc:postgresql:.*")
class PickupFeatureTest extends BackendFeatureTest {
    @Autowired com.uitmerch.backend.order.security.CheckoutSecurityService guestCheckoutSecurity;
    private com.uitmerch.backend.order.security.CheckoutSecurityService contextCheckoutSecurity() { return guestCheckoutSecurity; }
    @Autowired PickupService pickup;
    @Autowired PickupTokenRepository tokens;
    @Autowired OrderService orders;
    @Autowired OrderRepository orderRepository;
    @Autowired PickupScheduleRepository schedules;
    @Autowired OrderHistoryService history;
    private OrderResponse order(com.uitmerch.backend.organization.entity.Organization org, UUID user) {
        InstantOrderRequest request = new InstantOrderRequest(); request.setRequestId(UUID.randomUUID()); request.setMerchId(product(org, 3).getId()); request.setQuantity(1);
        return orders.createInstantOrder(user, request);
    }
    private void ready(com.uitmerch.backend.organization.entity.Organization org, UUID id) {
        orders.updateOrderStatus(org.getOwnerId(), org.getId(), id, OrderStatus.CONFIRMED);
        orders.updateOrderStatus(org.getOwnerId(), org.getId(), id, OrderStatus.READY);
    }
    @Test void qrIsHashedAndParallelCheckInCompletesOnceWithAudit() throws Exception {
        var org = organization(); var customer = user(UserRole.CUSTOMER); var order = order(org, customer.getId()); ready(org, order.getId());
        var issued = pickup.issue(customer.getId(), order.getId());
        assertThat(issued.token()).hasSize(43);
        assertThat(tokens.findById(order.getId()).orElseThrow().getTokenHash()).hasSize(64).isNotEqualTo(issued.token());
        pickup.verify(org.getOwnerId(), org.getId(), issued.token(), null);
        assertThat(tokens.findById(order.getId()).orElseThrow().getConsumedAt()).isNull();
        assertThat(parallel(2, () -> pickup.checkIn(org.getOwnerId(), org.getId(), issued.token(), null))).containsExactlyInAnyOrder(true, false);
        assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(tokens.findById(order.getId()).orElseThrow().getConsumedAt()).isNotNull();
        assertThat(history.forCustomer(customer.getId(), order.getId(), PageRequest.of(0, 20)).stream()
            .filter(h -> h.getSource().equals("QR") && h.getToStatus() == OrderStatus.COMPLETED).count()).isEqualTo(1);
    }
    @Test void reissueExpiryAndWrongOrganizationDenyCredentials() {
        var org = organization(); var another = organization(); var customer = user(UserRole.CUSTOMER);
        var order = order(org, customer.getId()); ready(org, order.getId());
        var old = pickup.issue(customer.getId(), order.getId()); var current = pickup.issue(customer.getId(), order.getId());
        assertThatThrownBy(() -> pickup.verify(org.getOwnerId(), org.getId(), old.token(), null)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> pickup.verify(another.getOwnerId(), another.getId(), current.token(), null)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> pickup.verify(another.getOwnerId(), org.getId(), current.token(), null)).isInstanceOf(ResourceNotFoundException.class);
        var entity = tokens.findById(order.getId()).orElseThrow(); entity.setExpiresAt(Instant.now().minusSeconds(1)); tokens.save(entity);
        assertThatThrownBy(() -> pickup.checkIn(org.getOwnerId(), org.getId(), current.token(), null)).isInstanceOf(ValidationException.class);
    }
    @Test void manualCompletionInvalidatesQrAndHistoryIsOwned() {
        var org = organization(); var customer = user(UserRole.CUSTOMER); var another = user(UserRole.CUSTOMER);
        var order = order(org, customer.getId()); ready(org, order.getId()); var qr = pickup.issue(customer.getId(), order.getId());
        orders.checkInOrder(org.getOwnerId(), org.getId(), order.getId());
        assertThatThrownBy(() -> pickup.checkIn(org.getOwnerId(), org.getId(), qr.token(), null)).isInstanceOf(ValidationException.class);
        assertThat(tokens.findById(order.getId()).orElseThrow().getRevokedAt()).isNotNull();
        assertThatThrownBy(() -> history.forCustomer(another.getId(), order.getId(), PageRequest.of(0, 20))).isInstanceOf(ResourceNotFoundException.class);
        assertThat(history.forCustomer(customer.getId(), order.getId(), PageRequest.of(0, 20)).stream()
            .filter(h -> h.getToStatus() == OrderStatus.COMPLETED).findFirst().orElseThrow().getSource()).isEqualTo("MANUAL");
    }
    @Test void scheduledQrRequiresMatchingScheduleAndCannotBeUsedEarly() {
        var org = organization(); var customer = user(UserRole.CUSTOMER); var order = order(org, customer.getId());
        orders.updateOrderStatus(org.getOwnerId(), org.getId(), order.getId(), OrderStatus.CONFIRMED);
        PickupScheduleRequest request = new PickupScheduleRequest(); request.setOrderIds(List.of(order.getId()));
        request.setPickupDate(LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusDays(1)); request.setPickupTimeSlot("09:00-10:00"); request.setLocation("UIT");
        var schedule = orders.createPickupSchedule(org.getOwnerId(), org.getId(), request); var qr = pickup.issue(customer.getId(), order.getId());
        assertThat(qr.pickupScheduleId()).isEqualTo(schedule.getId());
        assertThatThrownBy(() -> pickup.verify(org.getOwnerId(), org.getId(), qr.token(), null)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> pickup.verify(org.getOwnerId(), org.getId(), qr.token(), schedule.getId())).isInstanceOf(ValidationException.class);
        var row = schedules.findById(schedule.getId()).orElseThrow(); row.setPickupDate(LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"))); schedules.save(row);
        pickup.verify(org.getOwnerId(), org.getId(), qr.token(), schedule.getId());
    }
    @Test void rollbackPreservesTokenOrderAndAuditTogether() {
        var org = organization(); var customer = user(UserRole.CUSTOMER); var order = order(org, customer.getId()); ready(org, order.getId());
        var qr = pickup.issue(customer.getId(), order.getId()); long before = history.forCustomer(customer.getId(), order.getId(), PageRequest.of(0, 20)).getTotalElements();
        assertThatThrownBy(() -> tx.executeWithoutResult(s -> { pickup.checkIn(org.getOwnerId(), org.getId(), qr.token(), null); throw new IllegalStateException("rollback"); }))
            .isInstanceOf(IllegalStateException.class);
        assertThat(tokens.findById(order.getId()).orElseThrow().getConsumedAt()).isNull();
        assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.READY);
        assertThat(history.forCustomer(customer.getId(), order.getId(), PageRequest.of(0, 20)).getTotalElements()).isEqualTo(before);
    }
    @Test void issuanceAndScanApisEnforceRolesAndOrderOwnership() throws Exception {
        var org = organization(); var customer = user(UserRole.CUSTOMER); var stranger = user(UserRole.CUSTOMER);
        var order = order(org, customer.getId()); ready(org, order.getId());
        mvc.perform(post("/api/v1/customer/orders/" + order.getId() + "/pickup-token")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/customer/orders/" + order.getId() + "/pickup-token").header("Authorization", "Bearer " + token(stranger))).andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/customer/orders/" + order.getId() + "/pickup-token").header("Authorization", "Bearer " + token(customer)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.token").isString());
        mvc.perform(post("/api/v1/organizations/" + org.getId() + "/orders/pickup/checkin")
            .header("Authorization", "Bearer " + token(customer)).contentType("application/json").content("{\"token\":\"" + "A".repeat(43) + "\"}"))
            .andExpect(status().isForbidden());
    }
    @Test void guestMustProveAccessToEmailAndReceiptIsConsumedOnce() throws Exception {
        var org = organization(); var item = product(org, 3);
        GuestOrderRequest request = new GuestOrderRequest(); request.setRequestId(UUID.randomUUID()); request.setGuestName("Guest"); request.setGuestPhone("0901234567"); request.setGuestEmail("guest@uit.edu.vn");
        GuestOrderItemRequest line = new GuestOrderItemRequest(); line.setMerchId(item.getId()); line.setQuantity(1); request.setItems(List.of(line));
        var checkoutSecurity = contextCheckoutSecurity();
        UUID challenge = checkoutSecurity.challenge(request.getGuestEmail());
        var otp = json.readValue(jobs.findAll().getFirst().getPayload(), com.uitmerch.backend.common.delivery.BackgroundJobService.MailPayload.class);
        request.setGuestCheckoutToken(checkoutSecurity.verify(request.getGuestEmail(),challenge,otp.arguments().getFirst()));
        var order = orders.createGuestOrder(request).getFirst(); ready(org, order.getId()); jobs.deleteAll();
        String path = "/api/v1/public/orders/" + order.getId();
        mvc.perform(post(path + "/pickup-receipt").contentType("application/json").content("{\"email\":\"other@uit.edu.vn\"}"))
            .andExpect(status().isAccepted()).andExpect(jsonPath("$.data").doesNotExist());
        assertThat(jobs.count()).isZero();
        mvc.perform(post(path + "/pickup-receipt").contentType("application/json").content("{\"email\":\"guest@uit.edu.vn\"}"))
            .andExpect(status().isAccepted()).andExpect(jsonPath("$.data").doesNotExist());
        var payload = json.readValue(jobs.findAll().getFirst().getPayload(), com.uitmerch.backend.common.delivery.BackgroundJobService.MailPayload.class);
        assertThat(payload.recipient()).isEqualTo("guest@uit.edu.vn");
        String receipt = payload.arguments().getFirst();
        mvc.perform(post(path + "/pickup-token").contentType("application/json").content(json.writeValueAsString(Map.of("receiptToken", "A".repeat(43)))))
            .andExpect(status().isBadRequest());
        var issued = pickup.issueForGuest(order.getId(), receipt);
        assertThatThrownBy(() -> pickup.issueForGuest(order.getId(), receipt)).isInstanceOf(ValidationException.class);
        pickup.checkIn(org.getOwnerId(), org.getId(), issued.token(), null);
        assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }
}
