package com.uitmerch.backend.auth.service;

import com.uitmerch.backend.auth.entity.*;
import com.uitmerch.backend.auth.repository.AuthSessionRepository;
import com.uitmerch.backend.common.exception.AuthenticationException;
import com.uitmerch.backend.common.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AuthSessionService {
    private final AuthSessionRepository sessions;
    private final JwtTokenProvider jwt;
    public record Tokens(String accessToken, String refreshToken) {}

    @Transactional
    @org.springframework.scheduling.annotation.Scheduled(fixedDelay = 3_600_000)
    public void purgeExpiredSessions() {
        sessions.deleteExpiredBefore(Instant.now());
    }

    @Transactional
    public Tokens create(User user) {
        AuthSession session = new AuthSession();
        session.setId(UUID.randomUUID());
        session.setUserId(user.getId());
        return issue(session, user);
    }

    @Transactional
    public Tokens rotate(String refreshToken, User user) {
        AuthSession session = sessions.findLockedById(sessionId(refreshToken))
            .orElseThrow(AuthSessionService::invalidToken);
        if (!isUsable(session, user, refreshToken)
            || !MessageDigest.isEqual(session.getRefreshHash().getBytes(StandardCharsets.UTF_8),
                hash(refreshToken).getBytes(StandardCharsets.UTF_8))) {
            throw invalidToken();
        }
        return issue(session, user);
    }

    @Transactional(readOnly = true)
    public boolean isActive(String token, User user) {
        try {
            return sessions.findById(sessionId(token)).filter(s -> isUsable(s, user, token)).isPresent();
        } catch (IllegalArgumentException | AuthenticationException e) {
            return false;
        }
    }

    @Transactional
    public void revoke(String token) {
        String id = jwt.getSessionIdFromToken(token);
        if (id == null) return;
        sessions.findLockedById(UUID.fromString(id)).ifPresent(session -> {
            if (session.getUserId().toString().equals(jwt.getUserIdFromToken(token))) {
                session.setRevokedAt(Instant.now());
                sessions.save(session);
            }
        });
    }

    private boolean isUsable(AuthSession session, User user, String token) {
        return user.isActive() && user.isVerified() && session.getUserId().equals(user.getId())
            && session.getRevokedAt() == null && session.getExpiresAt().isAfter(Instant.now())
            && jwt.getAuthVersionFromToken(token) == user.getAuthVersion();
    }

    private Tokens issue(AuthSession session, User user) {
        String refresh = jwt.generateSessionRefreshToken(user.getId().toString(), session.getId().toString(), user.getAuthVersion());
        session.setRefreshHash(hash(refresh));
        session.setExpiresAt(jwt.getExpiryFromToken(refresh));
        sessions.save(session);
        String access = jwt.generateSessionAccessToken(user.getId().toString(), user.getEmail(), user.getRole().name(),
            session.getId().toString(), user.getAuthVersion());
        return new Tokens(access, refresh);
    }

    private UUID sessionId(String token) {
        try { return UUID.fromString(jwt.getSessionIdFromToken(token)); }
        catch (IllegalArgumentException | NullPointerException e) { throw invalidToken(); }
    }

    private static AuthenticationException invalidToken() {
        return new AuthenticationException("Invalid or expired token");
    }

    private static String hash(String token) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
