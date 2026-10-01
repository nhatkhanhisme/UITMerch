package com.uitmerch.backend.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class SseEmitterManager {

    private static final Logger log = LoggerFactory.getLogger(SseEmitterManager.class);
    private static final long SSE_TIMEOUT_MS = 30 * 60 * 1_000L;

    private final ConcurrentHashMap<UUID, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final com.uitmerch.backend.auth.repository.UserRepository users;
    private final com.uitmerch.backend.auth.repository.AuthSessionRepository sessions;
    private final ConcurrentHashMap<SseEmitter, StreamCredential> credentials = new ConcurrentHashMap<>();
    private record StreamCredential(UUID sessionId, long authVersion, java.time.Instant expiresAt) {}

    public SseEmitterManager(ObjectMapper objectMapper, com.uitmerch.backend.auth.repository.UserRepository users,
        com.uitmerch.backend.auth.repository.AuthSessionRepository sessions) {
        this.objectMapper = objectMapper;
        this.users = users;
        this.sessions = sessions;
    }

    public SseEmitter add(UUID userId, String sessionId, long authVersion, java.time.Instant expiresAt) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        credentials.put(emitter, new StreamCredential(UUID.fromString(sessionId), authVersion, expiresAt));
        emitters.compute(userId, (id, list) -> {
            if (list == null) list = new CopyOnWriteArrayList<>();
            list.add(emitter);
            return list;
        });

        Runnable cleanup = () -> remove(userId, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());

        try {
            emitter.send(SseEmitter.event().name("connect").data("connected"));
        } catch (IOException e) {
            log.warn("Failed to send SSE connect event to user {}: {}", userId, e.getMessage());
            cleanup.run();
        }

        return emitter;
    }

    public void send(UUID userId, Object data) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            // Defer until after the current transaction commits so the browser
            // re-fetches only after the new status is visible in the DB.
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    doSend(userId, data);
                }
            });
        } else {
            doSend(userId, data);
        }
    }

    private void doSend(UUID userId, Object data) {
        CopyOnWriteArrayList<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters == null || userEmitters.isEmpty()) return;

        String json;
        try {
            json = objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize SSE payload for user {}: {}", userId, e.getMessage());
            return;
        }

        List<SseEmitter> dead = new ArrayList<>();
        for (SseEmitter emitter : userEmitters) {
            try {
                if (!isAuthorized(userId, emitter)) {
                    emitter.complete();
                    dead.add(emitter);
                    continue;
                }
                emitter.send(SseEmitter.event().name("notification").data(json));
            } catch (IOException e) {
                dead.add(emitter);
            }
        }
        dead.forEach(e -> remove(userId, e));
    }

    @Scheduled(fixedDelay = 25_000)
    public void sendHeartbeat() {
        if (emitters.isEmpty()) return;
        emitters.forEach((userId, userEmitters) -> {
            List<SseEmitter> dead = new ArrayList<>();
            for (SseEmitter emitter : userEmitters) {
                try {
                    if (!isAuthorized(userId, emitter)) {
                        emitter.complete();
                        dead.add(emitter);
                        continue;
                    }
                    emitter.send(SseEmitter.event().comment("heartbeat"));
                } catch (IOException e) {
                    dead.add(emitter);
                }
            }
            dead.forEach(e -> remove(userId, e));
        });
    }

    private boolean isAuthorized(UUID userId, SseEmitter emitter) {
        StreamCredential credential = credentials.get(emitter);
        if (credential == null || !credential.expiresAt().isAfter(java.time.Instant.now())) return false;
        try {
            var user = users.findById(userId);
            var session = sessions.findById(credential.sessionId());
            return user.isPresent() && user.get().isActive() && user.get().isVerified()
                && user.get().getAuthVersion() == credential.authVersion()
                && session.isPresent() && session.get().getUserId().equals(userId)
                && session.get().getRevokedAt() == null && session.get().getExpiresAt().isAfter(java.time.Instant.now());
        } catch (org.springframework.dao.DataAccessException e) {
            log.warn("Closing notification stream after authentication storage failure");
            return false;
        }
    }

    private void remove(UUID userId, SseEmitter emitter) {
        credentials.remove(emitter);
        emitters.computeIfPresent(userId, (id, list) -> {
            list.remove(emitter);
            return list.isEmpty() ? null : list;
        });
    }
}
