package com.uitmerch.backend.regression;

import com.uitmerch.backend.common.delivery.*;
import com.uitmerch.backend.common.service.EmailService;
import com.uitmerch.backend.ai.service.*;
import com.uitmerch.backend.common.exception.ValidationException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.*;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@SpringBootTest(properties = {"app.jwt.secret=uitmerch-disposable-regression-test-secret-2026", "app.delivery.enabled=false"})
@ActiveProfiles("docker")
@EnabledIfEnvironmentVariable(named = "UITMERCH_TEST_DATABASE_URL", matches = "jdbc:postgresql:.*")
class DeliveryRegressionTest {
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("UITMERCH_TEST_DATABASE_URL"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "uitmerch_test_only");
    }
    @Autowired BackgroundJobService jobs;
    @Autowired BackgroundJobRepository repository;
    @Autowired BackgroundJobDispatcher dispatcher;
    @Autowired BackgroundJobClaimService claims;
    @Autowired EmailService mail;
    @Autowired TransactionTemplate transaction;
    @Autowired com.uitmerch.backend.auth.repository.UserRepository users;
    @Autowired com.uitmerch.backend.organization.repository.OrganizationRepository organizations;
    @Autowired com.uitmerch.backend.merch.repository.MerchItemRepository products;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    @MockBean(name = "mailTransport") EmailService transport;
    @MockBean EmbeddingService embedding;
    @MockBean MerchEmbeddingService merchEmbedding;

    @BeforeEach void clearJobs() { repository.deleteAll(); }
    @Test void rollbackCannotDeliverEmailOrCreateEmbeddingJobs() {
        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            mail.sendOtp("test@uit.edu.vn", "123456");
            jobs.enqueueEmbedding(UUID.randomUUID());
            throw new ValidationException("Rollback");
        })).isInstanceOf(ValidationException.class);
        assertThat(repository.count()).isZero();
        assertThat(dispatcher.dispatchNext()).isFalse();
        verifyNoInteractions(transport, embedding, merchEmbedding);
    }
    @Test void committedJobIsDeliveredAndSensitivePayloadIsRemoved() {
        mail.sendOtp("test@uit.edu.vn", "123456");
        assertThat(repository.count()).isEqualTo(1);
        verifyNoInteractions(transport);
        assertThat(dispatcher.dispatchNext()).isTrue();
        verify(transport).sendOtp("test@uit.edu.vn", "123456");
        var job = repository.findAll().getFirst();
        assertThat(job.getState()).isEqualTo("DONE");
        assertThat(job.getPayload()).isEmpty();
        assertThat(dispatcher.dispatchNext()).isFalse();
    }
    @Test void transportFailureIsRetriedWithPersistentAttemptCount() {
        doThrow(new IllegalStateException("Provider unavailable")).doNothing().when(transport).sendOtp(anyString(), anyString());
        mail.sendOtp("test@uit.edu.vn", "123456"); dispatcher.dispatchNext();
        var job = repository.findAll().getFirst();
        assertThat(job.getState()).isEqualTo("PENDING"); assertThat(job.getAttempts()).isEqualTo(1);
        assertThat(job.getNextAttemptAt()).isAfter(Instant.now());
        job.setNextAttemptAt(Instant.now().minusSeconds(1)); repository.save(job);
        dispatcher.dispatchNext();
        job = repository.findById(job.getId()).orElseThrow();
        assertThat(job.getState()).isEqualTo("DONE"); assertThat(job.getAttempts()).isEqualTo(2);
        verify(transport, times(2)).sendOtp("test@uit.edu.vn", "123456");
    }
    @Test void crashedWorkerLeaseCanBeRecovered() {
        mail.sendOtp("test@uit.edu.vn", "123456");
        var claimed = claims.claim().orElseThrow();
        assertThat(claims.claim()).isEmpty();
        claimed.setNextAttemptAt(Instant.now().minusSeconds(1)); repository.save(claimed);
        assertThat(dispatcher.dispatchNext()).isTrue();
        assertThat(repository.findById(claimed.getId()).orElseThrow().getAttempts()).isEqualTo(2);
        verify(transport).sendOtp("test@uit.edu.vn", "123456");
    }
    @Test void expiredOtpJobsAreDiscarded() {
        mail.sendOtp("test@uit.edu.vn", "123456");
        var job = repository.findAll().getFirst(); job.setCreatedAt(Instant.now().minusSeconds(901)); repository.save(job);
        dispatcher.dispatchNext();
        verifyNoInteractions(transport);
        assertThat(repository.findById(job.getId()).orElseThrow().getState()).isEqualTo("DONE");
    }
    @Test void repeatedFailuresStopAfterFiveAttempts() {
        doThrow(new IllegalStateException("Provider unavailable")).when(transport).sendOtp(anyString(), anyString());
        mail.sendOtp("test@uit.edu.vn", "123456");
        for (int i = 0; i < 5; i++) {
            var job = repository.findAll().getFirst(); job.setNextAttemptAt(Instant.now().minusSeconds(1)); repository.save(job);
            assertThat(dispatcher.dispatchNext()).isTrue();
        }
        var job = repository.findAll().getFirst();
        assertThat(job.getState()).isEqualTo("DEAD"); assertThat(job.getAttempts()).isEqualTo(5); assertThat(job.getPayload()).isEmpty();
        assertThat(dispatcher.dispatchNext()).isFalse();
        verify(transport, times(5)).sendOtp("test@uit.edu.vn", "123456");
    }
    @Test void concurrentWorkersCannotClaimTheSameLiveLease() throws Exception {
        mail.sendOtp("test@uit.edu.vn", "123456");
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var start = new java.util.concurrent.CountDownLatch(1);
            var first = executor.submit(() -> { start.await(); return claims.claim(); });
            var second = executor.submit(() -> { start.await(); return claims.claim(); });
            start.countDown();
            long claimed = List.of(first.get(10, java.util.concurrent.TimeUnit.SECONDS), second.get(10, java.util.concurrent.TimeUnit.SECONDS))
                .stream().filter(Optional::isPresent).count();
            assertThat(claimed).isEqualTo(1);
        }
    }
    @Test void embeddingJobsRollbackAndUseCurrentCommittedProductText() {
        var user = users.save(com.uitmerch.backend.auth.entity.User.builder().email(UUID.randomUUID() + "@uit.edu.vn")
            .passwordHash("not-used").fullName("Embedding test").role(com.uitmerch.backend.common.model.UserRole.ORGANIZER).build());
        var org = organizations.save(com.uitmerch.backend.organization.entity.Organization.builder().ownerId(user.getId())
            .name("Embedding test").status(com.uitmerch.backend.common.model.OrganizationStatus.ACTIVE).build());
        var item = products.save(com.uitmerch.backend.merch.entity.MerchItem.builder().orgId(org.getId()).name("Original title")
            .price(java.math.BigDecimal.ONE).stock(1).status(com.uitmerch.backend.common.model.MerchItemStatus.PUBLISHED).build());
        var productionQueue = new ProdMerchEmbeddingService(jdbc, embedding, jobs);
        assertThatThrownBy(() -> transaction.executeWithoutResult(tx -> {
            productionQueue.storeAsync(item.getId(), "Original title");
            throw new ValidationException("Rollback");
        })).isInstanceOf(ValidationException.class);
        assertThat(repository.count()).isZero();
        productionQueue.storeAsync(item.getId(), "Original title");
        item.setName("Updated title"); item.setDescription("Updated description"); products.save(item);
        float[] vector = new float[768]; vector[0] = 1;
        when(embedding.embed("Updated title Updated description")).thenReturn(vector);
        assertThat(dispatcher.dispatchNext()).isTrue();
        verify(merchEmbedding).store(item.getId(), vector);
        assertThat(repository.findAll().getFirst().getState()).isEqualTo("DONE");
    }
}
