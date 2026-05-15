package com.uitmerch.backend.common.util;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Extracts the real client IP from an HTTP request.
 *
 * X-Forwarded-For is only trusted when the direct connection (remoteAddr)
 * comes from a configured trusted proxy IP. Without configuration, the raw
 * remoteAddr is always returned — this prevents spoofing by clients who
 * inject an X-Forwarded-For header when there is no proxy in front.
 *
 * Configure via:
 *   app.proxy.trusted-ips=10.0.0.1,10.0.0.2
 */
@Slf4j
@Component
public class IpUtil {

    @Value("${app.proxy.trusted-ips:}")
    private String rawTrustedIps;

    private Set<String> trustedProxyIps;

    @PostConstruct
    void init() {
        trustedProxyIps = Arrays.stream(rawTrustedIps.split(","))
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .collect(Collectors.toSet());

        if (!trustedProxyIps.isEmpty()) {
            log.info("Rate limiter trusts X-Forwarded-For from proxies: {}", trustedProxyIps);
        }
    }

    public String extractClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();

        if (!trustedProxyIps.isEmpty() && trustedProxyIps.contains(remoteAddr)) {
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                return xff.split(",")[0].trim();
            }
        }

        return remoteAddr;
    }
}
