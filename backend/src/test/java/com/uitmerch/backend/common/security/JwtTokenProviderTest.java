package com.uitmerch.backend.common.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider provider;

    // 64-char secret (512 bits) satisfies HMAC-SHA256 minimum key length
    private static final String SECRET =
        "uitmerch-test-secret-key-must-be-at-least-256-bits-long-padding!";

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "secretKey", SECRET);
        ReflectionTestUtils.setField(provider, "accessTokenExpiration",  3_600_000L);
        ReflectionTestUtils.setField(provider, "refreshTokenExpiration", 86_400_000L);
    }

    // ── access token ─────────────────────────────────────────────────────────

    @Test
    void generateAccessToken_validateToken_returnsTrue() {
        String token = provider.generateAccessToken("user-id", "u@uit.edu.vn", "CUSTOMER");
        assertThat(provider.validateToken(token)).isTrue();
    }

    @Test
    void generateAccessToken_claimsRoundtrip() {
        String token = provider.generateAccessToken("abc-123", "u@uit.edu.vn", "ORGANIZER");

        assertThat(provider.getUserIdFromToken(token)).isEqualTo("abc-123");
        assertThat(provider.getEmailFromToken(token)).isEqualTo("u@uit.edu.vn");
        assertThat(provider.getRoleFromToken(token)).isEqualTo("ORGANIZER");
    }

    @Test
    void validateAsRefreshToken_onAccessToken_returnsFalse() {
        String accessToken = provider.generateAccessToken("id", "e@e.com", "CUSTOMER");
        assertThat(provider.validateAsRefreshToken(accessToken)).isFalse();
    }

    // ── refresh token ────────────────────────────────────────────────────────

    @Test
    void generateRefreshToken_validateAsRefreshToken_returnsTrue() {
        String refresh = provider.generateRefreshToken("user-id");
        assertThat(provider.validateAsRefreshToken(refresh)).isTrue();
    }

    @Test
    void validateToken_onRefreshToken_returnsTrue() {
        // Refresh tokens are valid JWTs — validateToken doesn't check type
        String refresh = provider.generateRefreshToken("user-id");
        assertThat(provider.validateToken(refresh)).isTrue();
    }

    @Test
    void validateAsRefreshToken_onAccessToken_discriminatesCorrectly() {
        String access  = provider.generateAccessToken("id", "e@e.com", "CUSTOMER");
        String refresh = provider.generateRefreshToken("id");

        assertThat(provider.validateAsRefreshToken(access)).isFalse();
        assertThat(provider.validateAsRefreshToken(refresh)).isTrue();
    }

    // ── tampered / invalid tokens ────────────────────────────────────────────

    @Test
    void validateToken_tamperedSignature_returnsFalse() {
        String token = provider.generateAccessToken("id", "e@e.com", "CUSTOMER");
        String tampered = token.substring(0, token.length() - 4) + "XXXX";
        assertThat(provider.validateToken(tampered)).isFalse();
    }

    @Test
    void validateToken_randomString_returnsFalse() {
        assertThat(provider.validateToken("not.a.jwt")).isFalse();
    }

    @Test
    void validateToken_emptyString_returnsFalse() {
        assertThat(provider.validateToken("")).isFalse();
    }

    // ── expiry ───────────────────────────────────────────────────────────────

    @Test
    void getExpiryFromToken_isInFuture() {
        String token = provider.generateAccessToken("id", "e@e.com", "CUSTOMER");
        assertThat(provider.getExpiryFromToken(token)).isAfter(java.time.Instant.now());
    }
}
