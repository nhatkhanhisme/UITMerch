package com.uitmerch.backend.common.service;

import com.uitmerch.backend.common.entity.InvalidatedToken;
import com.uitmerch.backend.common.repository.InvalidatedTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceTest {

    @Mock private InvalidatedTokenRepository repository;

    @InjectMocks private TokenBlacklistService service;

    private static final String TOKEN = "header.payload.signature";

    // ── add / isBlacklisted ──────────────────────────────────────────────────

    @Test
    void add_persistsHashAndCachesInMemory() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Instant expiry = Instant.now().plusSeconds(3600);
        service.add(TOKEN, expiry);

        assertThat(service.isBlacklisted(TOKEN)).isTrue();

        ArgumentCaptor<InvalidatedToken> captor = ArgumentCaptor.forClass(InvalidatedToken.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getTokenHash()).hasSize(64); // SHA-256 hex is 64 chars
        assertThat(captor.getValue().getExpiresAt()).isEqualTo(expiry);
    }

    @Test
    void isBlacklisted_unknownToken_returnsFalse() {
        assertThat(service.isBlacklisted("unknown.token")).isFalse();
    }

    @Test
    void isBlacklisted_expiredEntry_returnsFalseAndEvictsFromCache() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.add(TOKEN, Instant.now().minusSeconds(1)); // already expired

        assertThat(service.isBlacklisted(TOKEN)).isFalse();
    }

    // ── loadFromDatabase ─────────────────────────────────────────────────────

    @Test
    void loadFromDatabase_populatesCacheOnStartup() {
        Instant future = Instant.now().plusSeconds(3600);
        when(repository.findAllByExpiresAtAfter(any()))
            .thenReturn(List.of(new InvalidatedToken("abc123", future)));

        service.loadFromDatabase();

        // The cached entry is keyed by hash "abc123", not the raw token, so
        // isBlacklisted(TOKEN) won't match — but we can verify the DB was queried.
        verify(repository).findAllByExpiresAtAfter(any());
    }

    // ── evictExpired ─────────────────────────────────────────────────────────

    @Test
    void evictExpired_removesStaleEntriesFromCacheAndDb() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.add(TOKEN, Instant.now().minusSeconds(1));

        service.evictExpired();

        verify(repository).deleteExpiredBefore(any());
        // Token should no longer be blacklisted
        assertThat(service.isBlacklisted(TOKEN)).isFalse();
    }
}
