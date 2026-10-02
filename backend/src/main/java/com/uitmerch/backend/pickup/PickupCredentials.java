package com.uitmerch.backend.pickup;
import com.uitmerch.backend.common.exception.ValidationException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
final class PickupCredentials {
    private static final SecureRandom RANDOM = new SecureRandom();
    private PickupCredentials() {}
    static String generate() {
        byte[] bytes = new byte[32]; RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    static String hash(String token) {
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) throw invalid();
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    static boolean matches(String token, String hash) {
        return MessageDigest.isEqual(hash(token).getBytes(StandardCharsets.UTF_8), hash.getBytes(StandardCharsets.UTF_8));
    }
    static ValidationException invalid() { return new ValidationException("Invalid or expired pickup credential."); }
}
