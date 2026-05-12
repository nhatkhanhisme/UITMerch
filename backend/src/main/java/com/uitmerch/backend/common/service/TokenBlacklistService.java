package com.uitmerch.backend.common.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory token blacklist for logout support.
 * Tokens are evicted automatically once their natural expiry passes.
 * Not cluster-safe — suitable for single-instance deployments.
 * Replace with a Redis-backed implementation for multi-instance or zero-downtime deploys.
 */
@Service
public class TokenBlacklistService {

    private final ConcurrentHashMap<String, Instant> blacklist = new ConcurrentHashMap<>();

    public void add(String token, Instant expiry) {
        blacklist.put(token, expiry);
    }

    public boolean isBlacklisted(String token) {
        Instant expiry = blacklist.get(token);
        if (expiry == null) return false;
        if (expiry.isBefore(Instant.now())) {
            blacklist.remove(token);
            return false;
        }
        return true;
    }

    // Remove tokens whose JWT expiry has already passed — they can no longer be used anyway.
    @Scheduled(fixedRate = 3_600_000)
    public void evictExpired() {
        Instant now = Instant.now();
        blacklist.entrySet().removeIf(e -> e.getValue().isBefore(now));
    }
}
