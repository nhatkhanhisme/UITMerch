package com.uitmerch.backend.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

/**
 * JWT token provider for generation and validation.
 */
@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    private static final String ISSUER = "uitmerch";
    private static final String USER_ID_CLAIM = "userId";
    private static final String EMAIL_CLAIM = "email";
    private static final String ROLE_CLAIM = "role";

    @Value("${app.jwt.secret}")
    private String secretKey;

    @Value("${app.jwt.expiration:86400000}")
    private long accessTokenExpiration;

    @Value("${app.jwt.refresh-expiration:604800000}")
    private long refreshTokenExpiration;

    public String generateAccessToken(String userId, String email, String role) {
        return generateToken(
            Map.of(
                USER_ID_CLAIM, userId,
                EMAIL_CLAIM, email,
                ROLE_CLAIM, role
            ),
            accessTokenExpiration
        );
    }

    public String generateRefreshToken(String userId) {
        return generateToken(Map.of(USER_ID_CLAIM, userId), refreshTokenExpiration);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (io.jsonwebtoken.security.SecurityException e) {
            logger.warn("Invalid JWT signature: {}", e.getMessage());
            return false;
        } catch (MalformedJwtException e) {
            logger.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        } catch (ExpiredJwtException e) {
            logger.warn("Expired JWT token: {}", e.getMessage());
            return false;
        } catch (UnsupportedJwtException e) {
            logger.warn("Unsupported JWT token: {}", e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            logger.warn("JWT token is empty: {}", e.getMessage());
            return false;
        }
    }

    public String getUserIdFromToken(String token) {
        return getClaim(token, USER_ID_CLAIM, String.class);
    }

    public String getEmailFromToken(String token) {
        return getClaim(token, EMAIL_CLAIM, String.class);
    }

    public String getRoleFromToken(String token) {
        return getClaim(token, ROLE_CLAIM, String.class);
    }

    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    public Instant getAccessTokenExpiryInstant() {
        return Instant.now().plusMillis(accessTokenExpiration);
    }

    private String generateToken(Map<String, ?> claims, long expirationMs) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
            .claims(claims)
            .issuer(ISSUER)
            .issuedAt(now)
            .expiration(expiration)
            .signWith(getSigningKey())
            .compact();
    }

    private <T> T getClaim(String token, String claimName, Class<T> type) {
        return getAllClaims(token).get(claimName, type);
    }

    private Claims getAllClaims(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }
}
