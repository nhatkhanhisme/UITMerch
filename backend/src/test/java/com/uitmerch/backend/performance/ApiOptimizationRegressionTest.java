package com.uitmerch.backend.performance;

import com.uitmerch.backend.auth.dto.LoginRequest;
import com.uitmerch.backend.auth.entity.User;
import com.uitmerch.backend.auth.repository.UserRepository;
import com.uitmerch.backend.auth.service.AuthService;
import com.uitmerch.backend.cart.repository.CartRepository;
import com.uitmerch.backend.cart.service.CartService;
import com.uitmerch.backend.common.exception.ResourceNotFoundException;
import com.uitmerch.backend.common.exception.ValidationException;
import com.uitmerch.backend.common.model.UserRole;
import com.uitmerch.backend.merch.entity.Category;
import com.uitmerch.backend.merch.repository.CategoryRepository;
import com.uitmerch.backend.merch.service.CategoryService;
import com.uitmerch.backend.merch.service.MerchService;
import com.uitmerch.backend.order.dto.OrderResponse;
import com.uitmerch.backend.order.service.OrderService;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "app.jwt.secret=uitmerch-disposable-optimization-test-secret-2026",
    "app.delivery.enabled=false", "app.campaigns.enabled=false",
    "spring.jpa.properties.hibernate.generate_statistics=true",
    "logging.level.org.hibernate.stat=OFF",
    "logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener=OFF"
})
@ActiveProfiles("docker")
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "UITMERCH_TEST_DATABASE_URL", matches = "jdbc:postgresql://127\\.0\\.0\\.1:.*")
class ApiOptimizationRegressionTest {
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", () -> System.getenv("UITMERCH_TEST_DATABASE_URL"));
        properties.add("spring.datasource.username", () -> "postgres");
        properties.add("spring.datasource.password", () -> "uitmerch_test_only");
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired OrderService orders;
    @Autowired CartService carts;
    @Autowired CartRepository cartRepository;
    @Autowired UserRepository users;
    @Autowired CategoryRepository categories;
    @Autowired CategoryService categoryService;
    @Autowired MerchService merch;
    @Autowired CacheManager caches;
    @Autowired EntityManagerFactory emf;
    @Autowired TransactionTemplate transaction;
    @Autowired MockMvc mvc;
    @Autowired AuthService auth;
    @Autowired PasswordEncoder passwords;

    record Fixture(User customer, User owner, UUID orgId, UUID scheduleId) {}

    User user(UserRole role) {
        return users.save(User.builder().email(UUID.randomUUID() + "@uit.edu.vn")
            .passwordHash(passwords.encode("Password1")).fullName("Optimization Test")
            .role(role).isVerified(true).build());
    }

    Fixture fixture(int count) {
        User customer = user(UserRole.CUSTOMER), owner = user(UserRole.ORGANIZER);
        UUID orgId = UUID.randomUUID(), scheduleId = UUID.randomUUID(), merchId = UUID.randomUUID();
        jdbc.update("INSERT INTO organizations(id,owner_id,name,status) VALUES (?,?,'Optimization','ACTIVE')", orgId, owner.getId());
        jdbc.update("INSERT INTO pickup_schedules(id,org_id,pickup_date,pickup_time_slot,location) VALUES (?,?,CURRENT_DATE,'09:00-10:00','UIT')", scheduleId, orgId);
        jdbc.update("INSERT INTO merch_items(id,org_id,name,price,stock,status) VALUES (?,?,'Current product name',77000,1000,'PUBLISHED')", merchId, orgId);
        jdbc.update("INSERT INTO orders(id,user_id,org_id,pickup_schedule_id,total_amount,status) SELECT gen_random_uuid(),?,?,?,12000,'READY' FROM generate_series(1,?)", customer.getId(), orgId, scheduleId, count);
        jdbc.update("INSERT INTO order_items(id,order_id,merch_id,merch_name,unit_price,quantity,subtotal) SELECT gen_random_uuid(),id,?,'Historical name',12000,1,12000 FROM orders WHERE org_id=?", merchId, orgId);
        return new Fixture(customer, owner, orgId, scheduleId);
    }

    Statistics statistics() {
        return emf.unwrap(SessionFactory.class).getStatistics();
    }

    @Test
    void customerPagesHaveBoundedQueriesAndPreserveSnapshotsAndSchedules() {
        Fixture f = fixture(105);
        for (int size : List.of(1, 20, 100)) {
            statistics().clear();
            var page = orders.getCustomerOrders(f.customer().getId(), null, PageRequest.of(0, size));
            assertThat(statistics().getPrepareStatementCount()).isLessThanOrEqualTo(4);
            assertThat(page.getTotalElements()).isEqualTo(105);
            assertThat(page.getContent()).hasSize(size).allSatisfy(order -> {
                assertThat(order.getPickupSchedule().getId()).isEqualTo(f.scheduleId());
                assertThat(order.getItems()).singleElement().satisfies(item -> {
                    assertThat(item.getMerchName()).isEqualTo("Historical name");
                    assertThat(item.getUnitPrice()).isEqualByComparingTo("12000");
                });
            });
        }
    }

    @Test
    void organizerAndAdminPagesAlsoAvoidPerOrderQueries() {
        Fixture f = fixture(105);
        statistics().clear();
        var orgPage = orders.getOrgOrders(f.owner().getId(), f.orgId(), null, PageRequest.of(0, 100));
        assertThat(orgPage.getContent()).hasSize(100);
        assertThat(statistics().getPrepareStatementCount()).isLessThanOrEqualTo(6);
        statistics().clear();
        var adminPage = orders.getAllOrders(null, PageRequest.of(0, 100));
        assertThat(adminPage.getContent()).hasSize(100).allSatisfy(order -> assertThat(order.getPickupSchedule()).isNull());
        assertThat(statistics().getPrepareStatementCount()).isLessThanOrEqualTo(3);
    }

    @Test
    void emptyCustomerPageDoesNotLoadRelations() {
        User user = user(UserRole.CUSTOMER);
        statistics().clear();
        assertThat(orders.getCustomerOrders(user.getId(), null, PageRequest.of(0, 20))).isEmpty();
        assertThat(statistics().getPrepareStatementCount()).isEqualTo(1);
    }

    @Test
    void pickupPagesAreCappedAndHaveStableNonOverlappingBoundaries() {
        Fixture f = fixture(105);
        var first = orders.getPickupScheduleOrderPage(f.owner().getId(), f.orgId(), f.scheduleId(), PageRequest.of(0, 1000));
        var second = orders.getPickupScheduleOrderPage(f.owner().getId(), f.orgId(), f.scheduleId(), PageRequest.of(1, 100));
        assertThat(first.getSize()).isEqualTo(100);
        assertThat(first.getTotalElements()).isEqualTo(105);
        assertThat(first.getContent()).hasSize(100);
        assertThat(second.getContent()).hasSize(5);
        assertThat(first.getContent().stream().map(OrderResponse::getId).toList())
            .doesNotContainAnyElementsOf(second.getContent().stream().map(OrderResponse::getId).toList());
        assertThat(first.getContent()).allSatisfy(order -> assertThat(order.getPickupScheduleId()).isEqualTo(f.scheduleId()));
        assertThatThrownBy(() -> orders.getPickupScheduleOrderPage(f.owner().getId(), f.orgId(), f.scheduleId(),
            PageRequest.of(0, 20, Sort.by("guestEmail")))).isInstanceOf(ValidationException.class);
    }

    @Test
    void scheduleOwnershipAndCustomerOwnershipRemainEnforced() {
        Fixture f = fixture(1), other = fixture(1);
        assertThatThrownBy(() -> orders.getPickupScheduleOrderPage(other.owner().getId(), f.orgId(), f.scheduleId(), PageRequest.of(0, 20)))
            .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> orders.getPickupScheduleOrderPage(f.owner().getId(), f.orgId(), other.scheduleId(), PageRequest.of(0, 20)))
            .isInstanceOf(ResourceNotFoundException.class);
        UUID id = jdbc.queryForObject("SELECT id FROM orders WHERE org_id=?", UUID.class, f.orgId());
        assertThatThrownBy(() -> orders.getCustomerOrder(other.customer().getId(), id)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void scheduleCountsUseOneAggregateForTheEntirePage() {
        Fixture f = fixture(7);
        jdbc.update("INSERT INTO pickup_schedules(id,org_id,pickup_date,pickup_time_slot,location) SELECT gen_random_uuid(),?,CURRENT_DATE,'09:00-10:00','UIT' FROM generate_series(1,24)", f.orgId());
        statistics().clear();
        var page = orders.getPickupSchedules(f.owner().getId(), f.orgId(), PageRequest.of(0, 100));
        assertThat(statistics().getPrepareStatementCount()).isLessThanOrEqualTo(4);
        assertThat(page.getContent()).hasSize(25);
        assertThat(page.getContent().stream().mapToInt(value -> value.getOrderCount()).sum()).isEqualTo(7);
    }

    String accessToken(User user) {
        LoginRequest login = new LoginRequest(); login.setEmail(user.getEmail()); login.setPassword("Password1");
        return auth.login(login).getToken();
    }

    @Test
    void paginatedHttpContractIncludesMetadataAndLegacyListStaysComplete() throws Exception {
        Fixture f = fixture(23);
        String path = "/api/v1/organizations/" + f.orgId() + "/pickup-schedules/" + f.scheduleId() + "/orders";
        String token = "Bearer " + accessToken(f.owner());
        mvc.perform(get(path + "/page").header("Authorization", token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(20))
            .andExpect(jsonPath("$.meta.totalElements").value(23)).andExpect(jsonPath("$.meta.hasNext").value(true));
        mvc.perform(get(path).header("Authorization", token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(23));
        mvc.perform(get(path + "/page")).andExpect(status().isUnauthorized());
        mvc.perform(get(path + "/page").header("Authorization", "Bearer " + accessToken(f.customer())))
            .andExpect(status().isForbidden());
    }

    @Test
    void categoryCacheHitsAndEvictionHappensOnlyAfterCommit() {
        Category category = categories.save(Category.builder().slug("opt-" + UUID.randomUUID()).name("Before").displayOrder(999).build());
        caches.getCache("categories").clear();
        categoryService.listAll();
        statistics().clear();
        assertThat(categoryService.listAll()).anySatisfy(value -> assertThat(value.getId()).isEqualTo(category.getId()));
        assertThat(statistics().getPrepareStatementCount()).isZero();
        transaction.executeWithoutResult(status -> {
            Category update = categories.findById(category.getId()).orElseThrow();
            update.setName("Rolled back"); categories.save(update); status.setRollbackOnly();
        });
        assertThat(categoryService.listAll()).filteredOn(value -> value.getId().equals(category.getId()))
            .singleElement().satisfies(value -> assertThat(value.getName()).isEqualTo("Before"));
        transaction.executeWithoutResult(status -> {
            Category update = categories.findById(category.getId()).orElseThrow();
            update.setName("Committed"); categories.save(update);
        });
        assertThat(categoryService.listAll()).filteredOn(value -> value.getId().equals(category.getId()))
            .singleElement().satisfies(value -> assertThat(value.getName()).isEqualTo("Committed"));
    }

    @Test
    void readingAnExistingCartDoesNotWaitForTheUserWriteLock() throws Exception {
        User user = user(UserRole.CUSTOMER);
        UUID cartId = carts.getCart(user.getId()).getId();
        CountDownLatch locked = new CountDownLatch(1), release = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var holder = executor.submit(() -> transaction.executeWithoutResult(status -> {
                users.findLockedById(user.getId()).orElseThrow(); locked.countDown();
                try { assertThat(release.await(10, TimeUnit.SECONDS)).isTrue(); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
            }));
            try {
                assertThat(locked.await(5, TimeUnit.SECONDS)).isTrue();
                var read = executor.submit(() -> carts.getCart(user.getId()));
                assertThat(read.get(3, TimeUnit.SECONDS).getId()).isEqualTo(cartId);
            } finally { release.countDown(); }
            holder.get(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void parallelFirstCartReadsStillCreateExactlyOneCart() throws Exception {
        User user = user(UserRole.CUSTOMER);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(4)) {
            var tasks = java.util.stream.IntStream.range(0, 4).mapToObj(i -> executor.submit(() -> {
                start.await(); return carts.getCart(user.getId()).getId();
            })).toList();
            start.countDown();
            var ids = new java.util.HashSet<UUID>();
            for (var task : tasks) ids.add(task.get(15, TimeUnit.SECONDS));
            assertThat(ids).hasSize(1);
            assertThat(cartRepository.findByUserId(user.getId())).hasSize(1);
        }
    }

    @Test
    void newIndexesAreValidAndSubstringSearchKeepsPublicFiltering() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM pg_index i JOIN pg_class c ON c.oid=i.indexrelid WHERE c.relname IN ('idx_orders_user_created_id','idx_orders_org_pickup_created_id','idx_notifications_user_created_id','idx_merch_name_trgm') AND i.indisvalid AND i.indisready", Integer.class)).isEqualTo(4);
        Fixture f = fixture(0);
        String keyword = "needle-" + UUID.randomUUID();
        jdbc.update("INSERT INTO merch_items(id,org_id,name,price,stock,status) VALUES (gen_random_uuid(),?,?,1000,10,'PUBLISHED'),(gen_random_uuid(),?,?,1000,10,'ARCHIVED')", f.orgId(), keyword.toUpperCase(), f.orgId(), keyword);
        assertThat(merch.listPublished(keyword, null, PageRequest.of(0, 20)).getContent())
            .singleElement().satisfies(value -> assertThat(value.getName()).isEqualTo(keyword.toUpperCase()));
    }
}
