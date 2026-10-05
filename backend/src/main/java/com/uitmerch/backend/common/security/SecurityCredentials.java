package com.uitmerch.backend.common.security;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
public final class SecurityCredentials {
    private static final SecureRandom RANDOM=new SecureRandom();
    private SecurityCredentials() {}
    public static String generate() { byte[] bytes=new byte[32];RANDOM.nextBytes(bytes);return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
    public static String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
    public static boolean matches(String token,String expected) {
        return token!=null && token.matches("[A-Za-z0-9_-]{43}") && expected!=null
            && MessageDigest.isEqual(hash(token).getBytes(StandardCharsets.UTF_8),expected.getBytes(StandardCharsets.UTF_8));
    }
}
