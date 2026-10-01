package com.uitmerch.backend.common.delivery;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.delivery.enabled", havingValue = "true", matchIfMissing = true)
public class BackgroundJobScheduler {
    private final BackgroundJobDispatcher dispatcher;
    private final BackgroundJobClaimService claims;
    @Scheduled(fixedDelayString = "${app.delivery.poll-interval-ms:1000}")
    public void dispatch() { dispatcher.dispatchNext(); }
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanup() { claims.cleanup(); }
}
