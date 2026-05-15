package com.uitmerch.backend.common.service;

import com.uitmerch.backend.common.entity.InvalidatedToken;
import com.uitmerch.backend.common.repository.InvalidatedTokenRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Token blacklist backed by the invalidated_tokens PostgreSQL table.
 *
 * Tokens are stored as SHA-256 hashes (not the raw JWT) to save space.
 * An in-memory ConcurrentHashMap mirrors the DB so every authenticated
 * request only pays a hash computation + map lookup, not a DB query.
 * On startup the in-memory cache is populated from the DB, surviving
 * server restarts without losing logout state.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final InvalidatedTokenRepository repository;

    private final ConcurrentHashMap<String, Instant> cache = new ConcurrentHashMap<>();

    @PostConstruct
    void loadFromDatabase() {
        Instant now = Instant.now();
        repository.findAllByExpiresAtAfter(now)
            .forEach(t -> cache.put(t.getTokenHash(), t.getExpiresAt()));
        log.info("Token blacklist loaded {} active entries from DB", cache.size());
    }

    @Transactional
    public void add(String token, Instant expiry) {
        String hash = sha256(token);
        repository.save(new InvalidatedToken(hash, expiry));
        cache.put(hash, expiry);
    }

    public boolean isBlacklisted(String token) {
        String hash = sha256(token);
        Instant expiry = cache.get(hash);
        if (expiry == null) return false;
        if (expiry.isBefore(Instant.now())) {
            cache.remove(hash);
            return false;
        }
        return true;
    }

    @Transactional
    @Scheduled(fixedRate = 3_600_000)
    public void evictExpired() {
        Instant now = Instant.now();
        cache.entrySet().removeIf(e -> e.getValue().isBefore(now));
        repository.deleteExpiredBefore(now);
    }

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
