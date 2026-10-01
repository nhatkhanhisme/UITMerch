package com.uitmerch.backend.common.delivery;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class BackgroundJobClaimService {
    private static final int MAX_ATTEMPTS = 5;
    private final BackgroundJobRepository repository;

    @Transactional
    public Optional<BackgroundJob> claim() {
        var due = repository.findDue(Instant.now(), PageRequest.of(0, 1));
        if (due.isEmpty()) return Optional.empty();
        BackgroundJob job = due.getFirst();
        if (job.getAttempts() >= MAX_ATTEMPTS) {
            job.setState("DEAD"); job.setPayload("");
            return Optional.empty();
        }
        job.setState("PROCESSING");
        job.setAttempts(job.getAttempts() + 1);
        job.setNextAttemptAt(Instant.now().plusSeconds(300));
        return Optional.of(repository.save(job));
    }

    @Transactional
    public void finish(UUID id, int attempt, boolean success) {
        repository.findLockedById(id).ifPresent(job -> {
            if (!"PROCESSING".equals(job.getState()) || job.getAttempts() != attempt) return;
            job.setState(success ? "DONE" : job.getAttempts() >= MAX_ATTEMPTS ? "DEAD" : "PENDING");
            if ("DONE".equals(job.getState()) || "DEAD".equals(job.getState())) job.setPayload("");
            job.setNextAttemptAt(Instant.now().plusSeconds(Math.min(3600, 5L << job.getAttempts())));
            repository.save(job);
        });
    }

    @Transactional
    public void cleanup() { repository.deleteFinishedBefore(Instant.now().minusSeconds(30L * 86400)); }
}
