package com.uitmerch.backend.common.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.transaction.TransactionAwareCacheManagerProxy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/** Local metadata cache; bounded expiry also limits staleness across instances. */
@Configuration
public class CacheConfig {
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager caches = new CaffeineCacheManager("categories", "popular-merch");
        caches.setCaffeine(Caffeine.newBuilder().maximumSize(32).expireAfterWrite(Duration.ofSeconds(60)));
        // Repository write evictions run only after a successful outer commit.
        return new TransactionAwareCacheManagerProxy(caches);
    }
}
