package com.uitmerch.backend.regression;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uitmerch.backend.auth.dto.*;
import com.uitmerch.backend.auth.entity.*;
import com.uitmerch.backend.auth.repository.*;
import com.uitmerch.backend.auth.service.AuthService;
import com.uitmerch.backend.common.exception.*;
import com.uitmerch.backend.common.model.*;
import com.uitmerch.backend.common.service.EmailService;
import com.uitmerch.backend.ai.service.MerchEmbeddingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDateTime;
import org.springframework.data.domain.PageRequest;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"app.jwt.secret=uitmerch-disposable-regression-test-secret-2026", "app.delivery.enabled=false"})
@ActiveProfiles("docker")
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "UITMERCH_TEST_DATABASE_URL", matches = "jdbc:postgresql:.*")
class PostgresRegressionTest {
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("UITMERCH_TEST_DATABASE_URL"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "uitmerch_test_only");
    }
    @Autowired AuthService auth;
    @Autowired UserRepository users;
    @Autowired OtpTokenRepository otps;
    @Autowired PasswordEncoder passwords;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired com.uitmerch.backend.order.service.OrderService orders;
    @Autowired com.uitmerch.backend.order.repository.OrderRepository orderRepository;
    @Autowired com.uitmerch.backend.order.repository.OrderItemRepository orderItems;
    @Autowired com.uitmerch.backend.merch.repository.MerchItemRepository merchandise;
    @Autowired com.uitmerch.backend.merch.service.MerchService merchService;
    @Autowired com.uitmerch.backend.organization.repository.OrganizationRepository organizations;
    @Autowired com.uitmerch.backend.cart.service.CartService carts;
    @Autowired com.uitmerch.backend.wishlist.service.WishlistService wishlists;
    @Autowired com.uitmerch.backend.admin.service.AdminService admin;
    @Autowired com.uitmerch.backend.event.service.EventService events;
    @Autowired com.uitmerch.backend.event.repository.EventRepository eventRepository;
    @Autowired com.uitmerch.backend.event.repository.EventMerchRepository eventMerch;
    @Autowired org.springframework.transaction.support.TransactionTemplate transaction;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    @Autowired com.uitmerch.backend.common.security.JwtTokenProvider jwt;
    @Autowired com.uitmerch.backend.notification.service.SseEmitterManager streams;
    @MockBean EmailService email;
    @MockBean MerchEmbeddingService embeddings;

    private User user(boolean verified) {
        return users.save(User.builder().email(UUID.randomUUID() + "@uit.edu.vn")
            .passwordHash(passwords.encode("Password1")).fullName("Regression User")
            .role(UserRole.CUSTOMER).isVerified(verified).build());
    }
    private LoginRequest credentials(User user) {
        LoginRequest request = new LoginRequest();
        request.setEmail(user.getEmail()); request.setPassword("Password1");
        return request;
    }
    @Test
    void wrongOtpAttemptsCommitEvenWhenVerificationThrows() {
        User user = user(false);
        OtpToken otp = otps.save(OtpToken.builder().user(user).otpCode("123456")
            .expiresAt(LocalDateTime.now().plusMinutes(15)).build());
        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setEmail(user.getEmail()); request.setOtpCode("654321");
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> auth.verifyEmail(request)).isInstanceOf(InvalidOtpException.class);
        }
        OtpToken persisted = otps.findById(otp.getId()).orElseThrow();
        assertThat(persisted.getAttemptCount()).isEqualTo(5);
        assertThat(persisted.getLockedUntil()).isAfter(LocalDateTime.now());
        auth.resendOtp(user.getEmail());
        assertThat(otps.findById(otp.getId())).isPresent();
        request.setOtpCode("123456");
        assertThatThrownBy(() -> auth.verifyEmail(request)).isInstanceOf(InvalidOtpException.class);
        assertThat(users.findById(user.getId()).orElseThrow().isVerified()).isFalse();
    }
    @Test
    void deactivatedAccountCannotRefreshExistingToken() {
        User user = user(true);
        String refresh = auth.login(credentials(user)).getRefreshToken();
        user.setActive(false); users.save(user);
        assertThatThrownBy(() -> auth.refreshToken(refresh)).isInstanceOf(AuthenticationException.class);
    }
    @Test
    void guestItemValidationCascadesBeforeDatabaseWork() throws Exception {
        Map<String, Object> request = Map.of("guestName", "Student", "guestPhone", "0901234567",
            "items", List.of(Map.of("merchId", UUID.randomUUID(), "quantity", 0)));
        mvc.perform(post("/api/v1/public/orders").contentType("application/json")
            .content(json.writeValueAsString(request))).andExpect(status().isBadRequest());
        // Assert that the controller returns an item-level validation error, rather than availability errors.
        String response = mvc.perform(post("/api/v1/public/orders").contentType("application/json")
            .content(json.writeValueAsString(request))).andReturn().getResponse().getContentAsString();
        assertThat(response).contains("items[0].quantity");
    }

    private com.uitmerch.backend.organization.entity.Organization organization() {
        User owner = user(true);
        owner.setRole(UserRole.ORGANIZER); owner = users.save(owner);
        return organizations.save(com.uitmerch.backend.organization.entity.Organization.builder()
            .ownerId(owner.getId()).name("Regression " + UUID.randomUUID()).status(OrganizationStatus.ACTIVE).build());
    }
    private com.uitmerch.backend.merch.entity.MerchItem product(com.uitmerch.backend.organization.entity.Organization org, int stock) {
        return merchandise.save(com.uitmerch.backend.merch.entity.MerchItem.builder().orgId(org.getId())
            .name("Regression " + UUID.randomUUID()).stock(stock).price(new java.math.BigDecimal("100000"))
            .status(MerchItemStatus.PUBLISHED).build());
    }
    private com.uitmerch.backend.order.dto.InstantOrderRequest purchase(UUID id, int quantity) {
        var request = new com.uitmerch.backend.order.dto.InstantOrderRequest();
        request.setRequestId(UUID.randomUUID());request.setMerchId(id); request.setQuantity(quantity); return request;
    }
    private com.uitmerch.backend.order.dto.GuestOrderRequest publicPurchase(UUID id) {
        var request = new com.uitmerch.backend.order.dto.GuestOrderRequest();
        request.setRequestId(UUID.randomUUID());
        var item = new com.uitmerch.backend.order.dto.GuestOrderItemRequest();
        item.setMerchId(id); item.setQuantity(1);
        request.setItems(List.of(item)); request.setGuestName("Student"); request.setGuestPhone("0901234567");
        request.setGuestEmail("receipt@uit.edu.vn"); return request;
    }
    private List<Boolean> parallel(int count, Runnable action) throws Exception {
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(count)) {
            var start = new java.util.concurrent.CountDownLatch(1);
            List<java.util.concurrent.Future<Boolean>> futures = new ArrayList<>();
            for (int i = 0; i < count; i++) futures.add(executor.submit(() -> {
                start.await();
                try { action.run(); return true; }
                catch (AppException expected) { return false; }
            }));
            start.countDown();
            List<Boolean> results = new ArrayList<>();
            for (var future : futures) results.add(future.get(15, java.util.concurrent.TimeUnit.SECONDS));
            return results;
        }
    }
    @Test
    void parallelRefreshConsumesCredentialOnceAndLogoutRevokesItsSession() throws Exception {
        User user = user(true);
        var login = auth.login(credentials(user));
        List<Boolean> results = parallel(2, () -> auth.refreshToken(login.getRefreshToken()));
        assertThat(results).containsExactlyInAnyOrder(true, false);
        // Rotation preserves session access credentials; logout must revoke all refresh capability.
        auth.logout(login.getToken());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/customer/orders")
            .header("Authorization", "Bearer " + login.getToken())).andExpect(status().isUnauthorized());
        assertThatThrownBy(() -> auth.refreshToken(login.getRefreshToken())).isInstanceOf(AuthenticationException.class);
    }
    @Test
    void resetAttemptsCommitAndCodesStayBoundToTheirPurpose() {
        User user = user(true);
        OtpToken otp = otps.save(OtpToken.builder().user(user).otpCode("123456").passwordReset(true)
            .expiresAt(LocalDateTime.now().plusMinutes(15)).build());
        var verification = new VerifyEmailRequest(); verification.setEmail(user.getEmail()); verification.setOtpCode("123456");
        assertThatThrownBy(() -> auth.verifyEmail(verification)).isInstanceOf(InvalidOtpException.class);
        var reset = new ResetPasswordRequest(); reset.setEmail(user.getEmail()); reset.setOtpCode("000000"); reset.setNewPassword("NewPassword1");
        assertThatThrownBy(() -> auth.resetPassword(reset)).isInstanceOf(InvalidOtpException.class);
        assertThat(otps.findById(otp.getId()).orElseThrow().getAttemptCount()).isEqualTo(1);
        reset.setOtpCode("123456"); auth.resetPassword(reset);
        assertThat(users.findById(user.getId()).orElseThrow().getAuthVersion()).isEqualTo(1);
        assertThat(otps.findById(otp.getId()).orElseThrow().isUsed()).isTrue();
    }
    @Test
    void passwordResetRoleChangeAndDeactivationRejectExistingCredentials() throws Exception {
        User user = user(true); var login = auth.login(credentials(user));
        otps.save(OtpToken.builder().user(user).otpCode("123456").passwordReset(true)
            .expiresAt(LocalDateTime.now().plusMinutes(15)).build());
        var reset = new ResetPasswordRequest(); reset.setEmail(user.getEmail()); reset.setOtpCode("123456"); reset.setNewPassword("NewPassword1");
        auth.resetPassword(reset);
        assertThatThrownBy(() -> auth.refreshToken(login.getRefreshToken())).isInstanceOf(AuthenticationException.class);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/customer/orders")
            .header("Authorization", "Bearer " + login.getToken())).andExpect(status().isUnauthorized());
        User another = user(true); var second = auth.login(credentials(another));
        admin.updateUserRole(another.getId(), UserRole.ORGANIZER);
        assertThatThrownBy(() -> auth.refreshToken(second.getRefreshToken())).isInstanceOf(AuthenticationException.class);
        var third = auth.login(credentials(users.findById(another.getId()).orElseThrow()));
        admin.setUserActive(another.getId(), false);
        assertThatThrownBy(() -> auth.refreshToken(third.getRefreshToken())).isInstanceOf(AuthenticationException.class);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/public/merch")
            .header("Authorization", "Bearer " + third.getToken())).andExpect(status().isUnauthorized());
    }
    @Test
    void refreshTokensCannotAuthorizeRestAndPublicCheckoutUsesVerifiedIdentity() throws Exception {
        var org = organization(); var item = product(org, 5); User user = user(true);
        var login = auth.login(credentials(user)); String body = json.writeValueAsString(publicPurchase(item.getId()));
        mvc.perform(post("/api/v1/public/orders").header("Authorization", "Bearer " + login.getRefreshToken())
            .contentType("application/json").content(body)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/public/orders").header("Authorization", "Bearer malformed")
            .contentType("application/json").content(body)).andExpect(status().isUnauthorized());
        String linked = mvc.perform(post("/api/v1/public/orders").header("Authorization", "Bearer " + login.getToken())
            .contentType("application/json").content(body)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        UUID linkedId = UUID.fromString(json.readTree(linked).at("/data/0/id").asText());
        assertThat(orderRepository.findById(linkedId).orElseThrow().getUserId()).isEqualTo(user.getId());
        mvc.perform(post("/api/v1/public/orders").contentType("application/json").content(body))
            .andExpect(status().isUnauthorized());
        User owner = users.findById(org.getOwnerId()).orElseThrow();
        String organizerToken = auth.login(credentials(owner)).getToken();
        mvc.perform(post("/api/v1/public/orders").header("Authorization", "Bearer " + organizerToken)
            .contentType("application/json").content(body)).andExpect(status().isForbidden());
        assertThat(merchandise.findById(item.getId()).orElseThrow().getStock()).isEqualTo(4);
    }
    @Test
    void parallelCancellationsRestoreStockOnce() throws Exception {
        var org = organization(); var item = product(org, 5); User user = user(true);
        UUID order = orders.createInstantOrder(user.getId(), purchase(item.getId(), 2)).getId();
        var request = new com.uitmerch.backend.order.dto.CancelOrderRequest(); request.setCancelReason("Changed mind");
        assertThat(parallel(2, () -> orders.cancelCustomerOrder(user.getId(), order, request)))
            .containsExactlyInAnyOrder(true, false);
        assertThat(merchandise.findById(item.getId()).orElseThrow().getStock()).isEqualTo(5);
        assertThat(orderRepository.findById(order).orElseThrow().getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }
    @Test
    void cancellationAndConfirmationCannotBothWin() throws Exception {
        var org = organization(); var item = product(org, 5); User user = user(true);
        UUID order = orders.createInstantOrder(user.getId(), purchase(item.getId(), 1)).getId();
        var request = new com.uitmerch.backend.order.dto.CancelOrderRequest(); request.setCancelReason("Changed mind");
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var start = new java.util.concurrent.CountDownLatch(1);
            var cancel = executor.submit(() -> { start.await(); try { orders.cancelCustomerOrder(user.getId(), order, request); return true; } catch (ValidationException e) { return false; } });
            var confirm = executor.submit(() -> { start.await(); try { orders.updateOrderStatus(org.getOwnerId(), org.getId(), order, OrderStatus.CONFIRMED); return true; } catch (ValidationException e) { return false; } });
            start.countDown();
            assertThat(List.of(cancel.get(15, java.util.concurrent.TimeUnit.SECONDS), confirm.get(15, java.util.concurrent.TimeUnit.SECONDS)))
                .containsExactlyInAnyOrder(true, false);
        }
        var state = orderRepository.findById(order).orElseThrow().getStatus();
        assertThat(merchandise.findById(item.getId()).orElseThrow().getStock()).isEqualTo(state == OrderStatus.CANCELLED ? 5 : 4);
    }
    @Test
    void concurrentPurchasesCannotOversell() throws Exception {
        var item = product(organization(), 3); User user = user(true);
        List<Boolean> results = parallel(8, () -> orders.createInstantOrder(user.getId(), purchase(item.getId(), 1)));
        assertThat(results.stream().filter(Boolean::booleanValue).count()).isEqualTo(3);
        assertThat(merchandise.findById(item.getId()).orElseThrow().getStock()).isZero();
    }
    @Test
    void metadataEditHoldsStockLockUntilItsTransactionCommits() throws Exception {
        var org = organization(); var item = product(org, 5); User user = user(true);
        var editReady = new java.util.concurrent.CountDownLatch(1); var release = new java.util.concurrent.CountDownLatch(1);
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var edit = executor.submit(() -> transaction.executeWithoutResult(tx -> {
                var request = new com.uitmerch.backend.merch.dto.UpdateMerchRequest(); request.setDescription("Updated description");
                merchService.updateMerch(org.getOwnerId(), org.getId(), item.getId(), request);
                editReady.countDown();
                try { if (!release.await(5, java.util.concurrent.TimeUnit.SECONDS)) throw new IllegalStateException("Test release timed out"); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
            }));
            assertThat(editReady.await(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            var buy = executor.submit(() -> orders.createInstantOrder(user.getId(), purchase(item.getId(), 1)));
            try { assertThatThrownBy(() -> buy.get(200, java.util.concurrent.TimeUnit.MILLISECONDS)).isInstanceOf(java.util.concurrent.TimeoutException.class); }
            finally { release.countDown(); }
            edit.get(10, java.util.concurrent.TimeUnit.SECONDS); buy.get(10, java.util.concurrent.TimeUnit.SECONDS);
        }
        var current = merchandise.findById(item.getId()).orElseThrow();
        assertThat(current.getStock()).isEqualTo(4); assertThat(current.getDescription()).isEqualTo("Updated description");
    }
    @Test
    void multiOrganizationCheckoutPersistsEveryOrderItemAndNotification() {
        User user = user(true); var first = product(organization(), 5); var second = product(organization(), 5);
        var a = com.uitmerch.backend.cart.entity.CartItem.builder().merchId(first.getId()).quantity(1).build();
        var b = com.uitmerch.backend.cart.entity.CartItem.builder().merchId(second.getId()).quantity(2).build();
        var result = orders.createOrdersFromCart(user.getId(), null, List.of(a, b), null, null, null, null);
        assertThat(result).hasSize(2);
        for (var order : result) {
            assertThat(orderRepository.findById(order.getId())).isPresent();
            assertThat(orderItems.findByOrderId(order.getId())).hasSize(1);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM notifications WHERE related_order_id = ?", Integer.class, order.getId())).isEqualTo(2);
        }
        assertThat(merchandise.findById(first.getId()).orElseThrow().getStock()).isEqualTo(4);
        assertThat(merchandise.findById(second.getId()).orElseThrow().getStock()).isEqualTo(3);
    }
    @Test
    void failedCheckoutRollsBackAllOrderAndInventoryWrites() {
        var item = product(organization(), 5); User user = user(true);
        long count = orderRepository.count();
        assertThatThrownBy(() -> transaction.executeWithoutResult(tx -> {
            orders.createInstantOrder(user.getId(), purchase(item.getId(), 2));
            throw new ValidationException("Later business failure");
        })).isInstanceOf(ValidationException.class);
        assertThat(orderRepository.count()).isEqualTo(count);
        assertThat(merchandise.findById(item.getId()).orElseThrow().getStock()).isEqualTo(5);
    }
    @Test
    void concurrentFirstCartAndWishlistReadsCreateOnePersistentRecord() throws Exception {
        User user = user(true);
        assertThat(parallel(2, () -> carts.getCart(user.getId()))).containsOnly(true);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM carts WHERE user_id = ?", Integer.class, user.getId())).isEqualTo(1);
        assertThat(parallel(2, () -> wishlists.getWishlist(user.getId()))).containsOnly(true);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM wishlists WHERE user_id = ?", Integer.class, user.getId())).isEqualTo(1);
    }
    @Test
    void publicEventHidesUnpublishedProductsAndInactiveOrganizations() {
        var org = organization(); var published = product(org, 5); var draft = product(org, 5);
        draft.setStatus(MerchItemStatus.DRAFT); merchandise.save(draft);
        var event = eventRepository.save(com.uitmerch.backend.event.entity.Event.builder().orgId(org.getId())
            .title("Regression event").status(EventStatus.PUBLISHED).build());
        eventMerch.save(com.uitmerch.backend.event.entity.EventMerch.builder().eventId(event.getId()).merchId(published.getId()).build());
        eventMerch.save(com.uitmerch.backend.event.entity.EventMerch.builder().eventId(event.getId()).merchId(draft.getId()).build());
        assertThat(events.getPublicEvent(event.getId()).getMerch()).extracting(com.uitmerch.backend.merch.dto.MerchResponse::getId).containsExactly(published.getId());
        assertThat(events.getOwnEvent(org.getOwnerId(), org.getId(), event.getId()).getMerch()).hasSize(2);
        // Keep a published row while disabling its org, to verify query-time enforcement independently of archival.
        org.setStatus(OrganizationStatus.INACTIVE); organizations.save(org);
        assertThat(merchandise.findPublicByIds(List.of(published.getId()))).isEmpty();
        assertThat(events.getPublicEventsByOrg(org.getId(), PageRequest.of(0, 20))).isEmpty();
        assertThatThrownBy(() -> events.getPublicEvent(event.getId())).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> orders.createInstantOrder(user(true).getId(), purchase(published.getId(), 1))).isInstanceOf(ValidationException.class);
    }
    @Test
    void pickupRejectsDuplicateIdsNullIdsAndPastDates() {
        var org = organization(); var item = product(org, 5); User user = user(true);
        UUID id = orders.createInstantOrder(user.getId(), purchase(item.getId(), 1)).getId();
        orders.updateOrderStatus(org.getOwnerId(), org.getId(), id, OrderStatus.CONFIRMED);
        var request = new com.uitmerch.backend.order.dto.PickupScheduleRequest();
        request.setPickupDate(java.time.LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).plusDays(1));
        request.setPickupTimeSlot("08:00-10:00"); request.setLocation("UIT"); request.setOrderIds(List.of(id, id));
        assertThatThrownBy(() -> orders.createPickupSchedule(org.getOwnerId(), org.getId(), request)).isInstanceOf(ValidationException.class);
        request.setOrderIds(Arrays.asList(id, null));
        assertThatThrownBy(() -> orders.createPickupSchedule(org.getOwnerId(), org.getId(), request)).isInstanceOf(ValidationException.class);
        request.setOrderIds(List.of(id)); request.setPickupDate(java.time.LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).minusDays(1));
        assertThatThrownBy(() -> orders.createPickupSchedule(org.getOwnerId(), org.getId(), request)).isInstanceOf(ValidationException.class);
        assertThat(orderRepository.findById(id).orElseThrow().getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }
    @Test
    void revokedSessionsStopReceivingLiveNotifications() {
        User user = user(true); var login = auth.login(credentials(user));
        streams.add(user.getId(), jwt.getSessionIdFromToken(login.getToken()), user.getAuthVersion(), jwt.getExpiryFromToken(login.getToken()));
        auth.logout(login.getToken());
        streams.send(user.getId(), Map.of("private", "order update"));
        var connected = (Map<?, ?>) org.springframework.test.util.ReflectionTestUtils.getField(streams, "emitters");
        assertThat(connected.containsKey(user.getId())).isFalse();
    }
    @Test
    void organizationSuspensionAndArchivalBothPersistAcrossBulkClear() {
        var org = organization(); var item = product(org, 5);
        admin.updateOrganizationStatus(org.getId(), OrganizationStatus.INACTIVE);
        assertThat(organizations.findById(org.getId()).orElseThrow().getStatus()).isEqualTo(OrganizationStatus.INACTIVE);
        assertThat(merchandise.findById(item.getId()).orElseThrow().getStatus()).isEqualTo(MerchItemStatus.ARCHIVED);
    }
    @Test
    void popularityReflectsStockChangesAndArchivalImmediately() {
        product(organization(), 5);
        var popular = merchService.getPopularMerch();
        var selected = popular.stream().filter(m -> m.getStock() > 0).findFirst().orElseThrow();
        transaction.executeWithoutResult(tx -> merchandise.deductStock(selected.getId(), 1));
        assertThat(merchService.getPopularMerch().stream().filter(m -> m.getId().equals(selected.getId())).findFirst().orElseThrow().getStock())
            .isEqualTo(selected.getStock() - 1);
        var org = organizations.findById(selected.getOrgId()).orElseThrow();
        merchService.deleteMerch(org.getOwnerId(), org.getId(), selected.getId());
        assertThat(merchService.getPopularMerch()).noneMatch(m -> m.getId().equals(selected.getId()));
    }
    @Test
    void vectorSearchFiltersEligibilityBeforeLimitingResults() {
        var organization = organization(); var visible = product(organization, 5); var hidden = product(organization, 5);
        hidden.setStatus(MerchItemStatus.ARCHIVED); merchandise.save(hidden);
        var service = new com.uitmerch.backend.ai.service.ProdMerchEmbeddingService(jdbc,
            org.mockito.Mockito.mock(com.uitmerch.backend.ai.service.EmbeddingService.class),
            org.mockito.Mockito.mock(com.uitmerch.backend.common.delivery.BackgroundJobService.class));
        float[] exact = new float[768]; exact[0] = 1;
        float[] near = new float[768]; near[0] = 0.98f; near[1] = 0.02f;
        service.store(hidden.getId(), exact); service.store(visible.getId(), near);
        assertThat(service.findNearest(exact, 1)).extracting(com.uitmerch.backend.ai.service.MerchSimilarityEntry::id).containsExactly(visible.getId());
    }
    @Test
    void otpSubmissionLimitReturns429AndRetryAfter() throws Exception {
        String body = json.writeValueAsString(Map.of("email", UUID.randomUUID() + "@uit.edu.vn", "otpCode", "123456"));
        for (int i = 0; i < 10; i++) mvc.perform(post("/api/v1/auth/verify-email").contentType("application/json").content(body)
            .with(request -> { request.setRemoteAddr("127.0.0.42"); return request; })).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/auth/verify-email").contentType("application/json").content(body)
            .with(request -> { request.setRemoteAddr("127.0.0.42"); return request; })).andExpect(status().isTooManyRequests())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Retry-After", "900"));
    }
    @Test
    void publicVisualSearchLimitStopsAdditionalProviderWork() throws Exception {
        var output = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_RGB), "png", output);
        var image = new org.springframework.mock.web.MockMultipartFile("image", "image.png", "image/png", output.toByteArray());
        for (int i = 0; i < 5; i++) mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/api/v1/public/merch/visual-search")
            .file(image).with(request -> { request.setRemoteAddr("127.0.0.44"); return request; })).andExpect(status().isOk());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/api/v1/public/merch/visual-search")
            .file(image).with(request -> { request.setRemoteAddr("127.0.0.44"); return request; })).andExpect(status().isTooManyRequests())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Retry-After", "60"));
    }
    @Test
    void expiredAccessHeaderDoesNotPreventRefreshAndLogoutRejectsMissingCredentials() throws Exception {
        User user = user(true); var login = auth.login(credentials(user));
        String sessionId = jwt.getSessionIdFromToken(login.getToken());
        String expired;
        long expiry = jwt.getAccessTokenExpiration();
        try {
            org.springframework.test.util.ReflectionTestUtils.setField(jwt, "accessTokenExpiration", -1000L);
            expired = jwt.generateSessionAccessToken(user.getId().toString(), user.getEmail(), user.getRole().name(), sessionId, user.getAuthVersion());
        } finally { org.springframework.test.util.ReflectionTestUtils.setField(jwt, "accessTokenExpiration", expiry); }
        var csrfResponse=mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/auth/csrf")).andReturn().getResponse();
        String csrfToken=json.readTree(csrfResponse.getContentAsString()).path("data").path("csrfToken").asText();
        mvc.perform(post("/api/v1/auth/refresh").cookie(new jakarta.servlet.http.Cookie("uitmerch-refresh",login.getRefreshToken()),
                new jakarta.servlet.http.Cookie("uitmerch-csrf",csrfToken)).header("X-CSRF-TOKEN",csrfToken).header("Authorization", "Bearer " + expired)
            .contentType("application/json").content(json.writeValueAsString(Map.of("refreshToken", login.getRefreshToken()))))
            .andExpect(status().isOk());
        mvc.perform(post("/api/v1/auth/logout")).andExpect(status().isForbidden());
    }
}
