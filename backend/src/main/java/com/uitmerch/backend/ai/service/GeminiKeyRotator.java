package com.uitmerch.backend.ai.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Round-robin Gemini API key rotator.
 *
 * Configure via GEMINI_API_KEYS (comma-separated, preferred) or the legacy
 * GEMINI_API_KEY (single key, backward-compatible).
 *
 * On a 429 response callers invoke rotateAndGet() to advance to the next key
 * and retry. All key access is thread-safe.
 */
@Slf4j
@Component
@Profile("!(dev | docker)")
public class GeminiKeyRotator {

    private final List<String> keys;
    private final AtomicInteger index = new AtomicInteger(0);

    public GeminiKeyRotator(
        @Value("${app.ai.gemini-api-keys:}") String multiKeys,
        @Value("${app.ai.gemini-api-key:}") String singleKey
    ) {
        List<String> parsed = Arrays.stream(multiKeys.split(","))
            .map(String::trim)
            .filter(k -> !k.isBlank())
            .collect(Collectors.toList());

        if (parsed.isEmpty() && !singleKey.isBlank()) {
            parsed = List.of(singleKey);
        }

        this.keys = List.copyOf(parsed);

        if (keys.isEmpty()) {
            log.warn("GeminiKeyRotator: no API keys configured (set GEMINI_API_KEYS or GEMINI_API_KEY).");
        } else {
            log.info("GeminiKeyRotator: {} key(s) loaded.{}",
                keys.size(),
                keys.size() > 1 ? " Key rotation enabled." : "");
        }
    }

    public boolean hasKeys() {
        return !keys.isEmpty();
    }

    public int keyCount() {
        return keys.size();
    }

    /** Returns the currently active key. */
    public String currentKey() {
        return keys.get(index.get() % keys.size());
    }

    /**
     * Atomically advances to the next key and returns it.
     * Called when the current key receives a 429.
     */
    public String rotateAndGet() {
        int next = index.incrementAndGet();
        int slot = next % keys.size();
        log.warn("Gemini 429 — rotated to key slot {}/{}.", slot + 1, keys.size());
        return keys.get(slot);
    }
}
