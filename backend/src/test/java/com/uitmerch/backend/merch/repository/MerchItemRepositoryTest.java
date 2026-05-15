package com.uitmerch.backend.merch.repository;

import com.uitmerch.backend.auth.entity.User;
import com.uitmerch.backend.auth.repository.UserRepository;
import com.uitmerch.backend.common.model.MerchItemStatus;
import com.uitmerch.backend.common.model.OrganizationStatus;
import com.uitmerch.backend.common.model.UserRole;
import com.uitmerch.backend.merch.entity.MerchItem;
import com.uitmerch.backend.organization.entity.Organization;
import com.uitmerch.backend.organization.repository.OrganizationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("dev")
class MerchItemRepositoryTest {

    @Autowired private MerchItemRepository merchItemRepository;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private UserRepository userRepository;

    private UUID orgId;

    @BeforeEach
    void setUp() {
        User owner = userRepository.save(User.builder()
            .email("owner-" + UUID.randomUUID() + "@test.com")
            .passwordHash("hash")
            .fullName("Owner")
            .role(UserRole.ORGANIZER)
            .build());

        Organization org = organizationRepository.save(Organization.builder()
            .ownerId(owner.getId())
            .name("Test Org")
            .status(OrganizationStatus.ACTIVE)
            .build());

        orgId = org.getId();
    }

    private MerchItem savedMerch(int stock, MerchItemStatus status) {
        return merchItemRepository.save(MerchItem.builder()
            .orgId(orgId)
            .name("Item " + UUID.randomUUID())
            .price(BigDecimal.valueOf(50_000))
            .stock(stock)
            .status(status)
            .build());
    }

    // ── deductStock ──────────────────────────────────────────────────────

    @Test
    void deductStock_sufficientStock_returnsOneAndDeducts() {
        MerchItem item = savedMerch(10, MerchItemStatus.PUBLISHED);

        int updated = merchItemRepository.deductStock(item.getId(), 3);

        assertThat(updated).isEqualTo(1);
        assertThat(merchItemRepository.findById(item.getId()).get().getStock()).isEqualTo(7);
    }

    @Test
    void deductStock_insufficientStock_returnsZeroAndLeavesSame() {
        MerchItem item = savedMerch(2, MerchItemStatus.PUBLISHED);

        int updated = merchItemRepository.deductStock(item.getId(), 5);

        assertThat(updated).isEqualTo(0);
        assertThat(merchItemRepository.findById(item.getId()).get().getStock()).isEqualTo(2);
    }

    @Test
    void deductStock_concurrent_neverGoesNegative() throws InterruptedException {
        MerchItem item = savedMerch(5, MerchItemStatus.PUBLISHED);
        int threads = 10;
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    latch.await();
                    if (merchItemRepository.deductStock(item.getId(), 1) == 1) {
                        successes.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        latch.countDown();
        pool.shutdown();
        pool.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);

        int remaining = merchItemRepository.findById(item.getId()).get().getStock();
        assertThat(successes.get()).isEqualTo(5);
        assertThat(remaining).isEqualTo(0);
    }

    // ── restoreStock ─────────────────────────────────────────────────────

    @Test
    void restoreStock_addsQuantityBack() {
        MerchItem item = savedMerch(3, MerchItemStatus.PUBLISHED);

        merchItemRepository.restoreStock(item.getId(), 4);

        assertThat(merchItemRepository.findById(item.getId()).get().getStock()).isEqualTo(7);
    }

    // ── archivePublishedByOrgId ──────────────────────────────────────────

    @Test
    void archivePublishedByOrgId_archivesOnlyPublishedItems() {
        MerchItem published = savedMerch(1, MerchItemStatus.PUBLISHED);
        MerchItem draft     = savedMerch(1, MerchItemStatus.DRAFT);
        MerchItem archived  = savedMerch(1, MerchItemStatus.ARCHIVED);

        int count = merchItemRepository.archivePublishedByOrgId(orgId);

        assertThat(count).isEqualTo(1);
        assertThat(merchItemRepository.findById(published.getId()).get().getStatus()).isEqualTo(MerchItemStatus.ARCHIVED);
        assertThat(merchItemRepository.findById(draft.getId()).get().getStatus()).isEqualTo(MerchItemStatus.DRAFT);
        assertThat(merchItemRepository.findById(archived.getId()).get().getStatus()).isEqualTo(MerchItemStatus.ARCHIVED);
    }
}
