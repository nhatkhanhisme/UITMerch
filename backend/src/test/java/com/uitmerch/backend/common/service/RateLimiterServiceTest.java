package com.uitmerch.backend.common.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterServiceTest {

    private RateLimiterService service;

    @BeforeEach
    void setUp() {
        service = new RateLimiterService();
    }

    @Test
    void isAllowed_withinLimit_returnsTrue() {
        assertThat(service.isAllowed("key1", 3, Duration.ofMinutes(1))).isTrue();
        assertThat(service.isAllowed("key1", 3, Duration.ofMinutes(1))).isTrue();
        assertThat(service.isAllowed("key1", 3, Duration.ofMinutes(1))).isTrue();
    }

    @Test
    void isAllowed_atLimitOnNextAttempt_returnsFalse() {
        for (int i = 0; i < 3; i++) {
            service.isAllowed("key2", 3, Duration.ofMinutes(1));
        }
        assertThat(service.isAllowed("key2", 3, Duration.ofMinutes(1))).isFalse();
    }

    @Test
    void isAllowed_differentKeys_haveIndependentLimits() {
        for (int i = 0; i < 3; i++) {
            service.isAllowed("keyA", 3, Duration.ofMinutes(1));
        }
        // keyA is at limit, keyB should still be allowed
        assertThat(service.isAllowed("keyA", 3, Duration.ofMinutes(1))).isFalse();
        assertThat(service.isAllowed("keyB", 3, Duration.ofMinutes(1))).isTrue();
    }

    @Test
    void isAllowed_windowExpiry_resetsCounter() throws InterruptedException {
        // Use a tiny window so we can test expiry without long sleeps
        for (int i = 0; i < 2; i++) {
            service.isAllowed("key3", 2, Duration.ofMillis(100));
        }
        assertThat(service.isAllowed("key3", 2, Duration.ofMillis(100))).isFalse();

        Thread.sleep(150); // wait for window to expire

        assertThat(service.isAllowed("key3", 2, Duration.ofMillis(100))).isTrue();
    }

    @Test
    void isAllowed_zeroAttempts_immediatelyFalse() {
        assertThat(service.isAllowed("key4", 0, Duration.ofMinutes(1))).isFalse();
    }
}
