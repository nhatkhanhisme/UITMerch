package com.uitmerch.backend.auth.service;

import com.uitmerch.backend.common.exception.*;
import com.uitmerch.backend.common.security.*;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Service
public class AuthCookieService {
    public static final String REFRESH = "uitmerch-refresh";
    public static final String CSRF = "uitmerch-csrf";
    private static final String PATH = "/api/v1/auth";
    private final JwtTokenProvider jwt;
    private final AuthSessionService sessions;
    private final boolean secure;
    private final Set<String> origins;
    private final byte[] signingKey;
    public AuthCookieService(JwtTokenProvider jwt, AuthSessionService sessions,
        @Value("${app.auth.cookie-secure:true}") boolean secure,
        @Value("${app.cors.allowed-origins:}") String origins,
        @Value("${app.jwt.secret}") String secret) {
        this.jwt=jwt;this.sessions=sessions;this.secure=secure;
        this.origins=new HashSet<>(Arrays.stream(origins.split(",")).map(String::trim).filter(s->!s.isEmpty()).toList());
        this.signingKey=secret.getBytes(StandardCharsets.UTF_8);
    }
    public void origin(HttpServletRequest request) {
        String origin=request.getHeader("Origin");
        if ((origin!=null && !origins.contains(origin)) || "cross-site".equals(request.getHeader("Sec-Fetch-Site")))
            throw forbidden();
    }
    public String issueCsrf(HttpServletRequest request, HttpServletResponse response) {
        origin(request);
        String value=SecurityCredentials.generate()+"."+Instant.now().plusSeconds(7200).getEpochSecond();
        String token=value+"."+sign(value);
        cookie(response,CSRF,token,Duration.ofHours(2));
        response.setHeader("Cache-Control","no-store");
        return token;
    }
    public void csrf(HttpServletRequest request) {
        origin(request);
        String cookie=value(request,CSRF), header=request.getHeader("X-CSRF-TOKEN");
        if (cookie==null || header==null || cookie.length()>180 || !equal(cookie,header)) throw forbidden();
        try {
            String[] parts=cookie.split("\\.");
            if (parts.length!=3 || !parts[0].matches("[A-Za-z0-9_-]{43}")
                || Long.parseLong(parts[1])<=Instant.now().getEpochSecond()
                || !equal(parts[2],sign(parts[0]+"."+parts[1]))) throw forbidden();
        } catch (NumberFormatException ex) { throw forbidden(); }
    }
    public String refresh(HttpServletRequest request) {
        String token=value(request,REFRESH);
        if (token==null || token.isBlank()) throw new AuthenticationException("Invalid or expired session");
        return token;
    }
    public void write(HttpServletResponse response,String refresh) {
        cookie(response,REFRESH,refresh,Duration.between(Instant.now(),jwt.getExpiryFromToken(refresh)));
        response.setHeader("Cache-Control","no-store");
    }
    public void logout(HttpServletRequest request,HttpServletResponse response) {
        String token=value(request,REFRESH);
        if (token!=null && jwt.validateAsRefreshToken(token)) sessions.revoke(token);
        cookie(response,REFRESH,"",Duration.ZERO);
        cookie(response,CSRF,"",Duration.ZERO);
        response.setHeader("Cache-Control","no-store");
    }
    private String value(HttpServletRequest request,String name) {
        String found=null;
        if (request.getCookies()!=null) for (Cookie cookie:request.getCookies()) if(name.equals(cookie.getName())) {
            if (found!=null) throw forbidden();
            found=cookie.getValue();
        }
        return found;
    }
    private void cookie(HttpServletResponse response,String name,String value,Duration age) {
        response.addHeader(HttpHeaders.SET_COOKIE,ResponseCookie.from(name,value).httpOnly(true).secure(secure)
            .sameSite("Lax").path(PATH).maxAge(age.isNegative()?Duration.ZERO:age).build().toString());
    }
    private String sign(String value) {
        try {
            Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(signingKey,"HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(("uitmerch-csrf:"+value).getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException ex) { throw new IllegalStateException(ex); }
    }
    private boolean equal(String a,String b) { return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8),b.getBytes(StandardCharsets.UTF_8)); }
    private AppException forbidden() { return new AppException("Invalid request verification",HttpStatus.FORBIDDEN,"CSRF_REJECTED"); }
}
