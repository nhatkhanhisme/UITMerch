package com.uitmerch.backend.common.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory sliding-window rate limiter.
 * Tracks the timestamps of recent attempts per key (e.g. "login:<IP>").
 * Thread-safe; evicts stale entries hourly so the map does not grow unbounded.
 */
@Service
public class RateLimiterService {

    private final ConcurrentHashMap<String, Deque<Instant>> windowMap = new ConcurrentHashMap<>();

    /**
     * Returns true if the caller is within the allowed rate, false if the limit is exceeded.
     *
     * @param key         Identifies the caller + action (e.g. "login:192.168.1.1")
     * @param maxAttempts Maximum number of attempts allowed within the window
     * @param window      Rolling time window
     */
    public boolean isAllowed(String key, int maxAttempts, Duration window) {
        Instant now = Instant.now();
        Instant cutoff = now.minus(window);

        Deque<Instant> timestamps = windowMap.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(cutoff)) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= maxAttempts) {
                return false;
            }
            timestamps.addLast(now);
            return true;
        }
    }

    @Scheduled(fixedRate = 3_600_000)
    public void evictStale() {
        Instant cutoff = Instant.now().minusSeconds(3600);
        windowMap.entrySet().removeIf(entry -> {
            synchronized (entry.getValue()) {
                Instant last = entry.getValue().peekLast();
                return last == null || last.isBefore(cutoff);
            }
        });
    }
}
