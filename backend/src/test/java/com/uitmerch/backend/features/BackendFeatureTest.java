package com.uitmerch.backend.features;

import com.uitmerch.backend.auth.entity.User;
import com.uitmerch.backend.auth.dto.LoginRequest;
import com.uitmerch.backend.auth.repository.UserRepository;
import com.uitmerch.backend.auth.service.AuthService;
import com.uitmerch.backend.common.model.*;
import com.uitmerch.backend.common.service.EmailService;
import com.uitmerch.backend.common.delivery.*;
import com.uitmerch.backend.merch.entity.MerchItem;
import com.uitmerch.backend.merch.repository.MerchItemRepository;
import com.uitmerch.backend.merch.service.MerchService;
import com.uitmerch.backend.organization.entity.Organization;
import com.uitmerch.backend.organization.repository.OrganizationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.util.*;

@SpringBootTest(properties = {"app.jwt.secret=uitmerch-disposable-feature-test-secret-2026", "app.delivery.enabled=false"})
@ActiveProfiles("docker") @AutoConfigureMockMvc
abstract class BackendFeatureTest {
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("UITMERCH_TEST_DATABASE_URL"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "uitmerch_test_only");
    }
    @Autowired UserRepository users;
    @Autowired AuthService auth;
    @Autowired PasswordEncoder passwords;
    @Autowired MerchItemRepository merch;
    @Autowired MerchService merchandise;
    @Autowired OrganizationRepository organizations;
    @Autowired BackgroundJobRepository jobs;
    @Autowired BackgroundJobDispatcher dispatcher;
    @Autowired TransactionTemplate tx;
    @Autowired ObjectMapper json;
    @Autowired MockMvc mvc;
    @MockBean(name = "mailTransport") EmailService transport;
    @MockBean com.uitmerch.backend.ai.service.MerchEmbeddingService embeddings;

    @BeforeEach void clearQueuedJobs() { jobs.deleteAll(); }
    User user(UserRole role) {
        return users.save(User.builder().email(UUID.randomUUID() + "@uit.edu.vn").fullName("Feature User")
            .passwordHash(passwords.encode("Password1")).role(role).isVerified(true).build());
    }
    Organization organization() {
        User owner = user(UserRole.ORGANIZER);
        return organizations.save(Organization.builder().ownerId(owner.getId()).name("Feature " + UUID.randomUUID())
            .status(OrganizationStatus.ACTIVE).build());
    }
    MerchItem product(Organization org, int stock) {
        return merch.save(MerchItem.builder().orgId(org.getId()).name("Product " + UUID.randomUUID())
            .stock(stock).price(new BigDecimal("100000")).status(MerchItemStatus.PUBLISHED).build());
    }
    String token(User user) {
        LoginRequest login = new LoginRequest(); login.setEmail(user.getEmail()); login.setPassword("Password1");
        return auth.login(login).getToken();
    }
    void drain() {
        for (int i = 0; i < 200; i++) if (!dispatcher.dispatchNext()) return;
        throw new AssertionError("Job queue did not drain");
    }
    List<Boolean> parallel(int count, Runnable operation) throws Exception {
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(count)) {
            var start = new java.util.concurrent.CountDownLatch(1);
            List<java.util.concurrent.Future<Boolean>> futures = new ArrayList<>();
            for (int i = 0; i < count; i++) futures.add(executor.submit(() -> {
                start.await();
                try { operation.run(); return true; }
                catch (com.uitmerch.backend.common.exception.AppException expected) { return false; }
            }));
            start.countDown(); List<Boolean> results = new ArrayList<>();
            for (var future : futures) results.add(future.get(20, java.util.concurrent.TimeUnit.SECONDS));
            return results;
        }
    }
}
