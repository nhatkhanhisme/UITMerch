package com.uitmerch.backend.common.config;

import com.uitmerch.backend.common.security.JwtTokenProvider;
import com.uitmerch.backend.common.service.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * JWT Authentication Filter.
 * Intercepts every request, extracts JWT from Authorization header,
 * validates token, and sets Spring Security authentication.
 * 
 * NFR02: Strict role checks at Controller/Route level using @PreAuthorize("hasRole(...)").
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;
    private final com.uitmerch.backend.auth.repository.UserRepository userRepository;
    private final com.uitmerch.backend.auth.service.AuthSessionService authSessionService;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                   TokenBlacklistService tokenBlacklistService,
                                   com.uitmerch.backend.auth.repository.UserRepository userRepository,
                                   com.uitmerch.backend.auth.service.AuthSessionService authSessionService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.tokenBlacklistService = tokenBlacklistService;
        this.userRepository = userRepository;
        this.authSessionService = authSessionService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // These endpoints authenticate their own credentials; stale browser access
        // headers must not prevent login or refresh. Logout still validates its bearer.
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return java.util.Set.of("/api/v1/auth/login", "/api/v1/auth/refresh",
            "/api/v1/auth/register", "/api/v1/auth/register/organizer",
            "/api/v1/auth/verify-email", "/api/v1/auth/resend-otp",
            "/api/v1/auth/forgot-password", "/api/v1/auth/reset-password").contains(path);
    }
    
    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String jwt = extractToken(request);
            
            if (jwt != null) {
                if (!jwtTokenProvider.validateAsAccessToken(jwt) || tokenBlacklistService.isBlacklisted(jwt)) {
                    throw new org.springframework.security.authentication.BadCredentialsException("Invalid token");
                }
                // Extract claims and set authentication
                String userId = jwtTokenProvider.getUserIdFromToken(jwt);
                var user = userRepository.findById(java.util.UUID.fromString(userId))
                    .orElseThrow(() -> new org.springframework.security.authentication.BadCredentialsException("Invalid token"));
                if (!authSessionService.isActive(jwt, user)) {
                    throw new org.springframework.security.authentication.BadCredentialsException("Invalid session");
                }
                String email = user.getEmail();
                String role = user.getRole().name();
                
                // Create authorities with "ROLE_" prefix for hasRole() matching
                Collection<SimpleGrantedAuthority> authorities = new ArrayList<>();
                if (role != null) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                }
                
                // Create authentication token
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                    email,
                    null,
                    authorities
                );
                
                if (SecurityContextHolder.getContext().getAuthentication() == null) {
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
                
                // Store userId and role in request attributes for controller access
                request.setAttribute("userId", userId);
                request.setAttribute("email", email);
                request.setAttribute("role", role);
                request.setAttribute("sessionId", jwtTokenProvider.getSessionIdFromToken(jwt));
                request.setAttribute("authVersion", user.getAuthVersion());
                request.setAttribute("accessExpiresAt", jwtTokenProvider.getExpiryFromToken(jwt));
            }
        } catch (Exception e) {
            SecurityContextHolder.clearContext();
            logger.debug("JWT authentication failed: {}", e.getClass().getSimpleName());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"message\":\"Invalid or expired credentials\"}");
            return;
        }
        
        filterChain.doFilter(request, response);
    }
    
    /**
     * Extract JWT token from Authorization header.
     * Expected format: "Bearer <token>"
     * @param request HTTP request
     * @return JWT token or null if not found
     */
    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }
        if (authHeader != null) {
            throw new org.springframework.security.authentication.BadCredentialsException("Invalid Authorization header");
        }
        // EventSource cannot set custom headers, so SSE endpoints accept token as a query param.
        String path = request.getServletPath();
        if ("/api/v1/customer/notifications/stream".equals(path)
                || "/api/v1/organizer/notifications/stream".equals(path)) {
            String queryToken = request.getParameter("token");
            if (queryToken != null && !queryToken.isBlank()) {
                return queryToken;
            }
        }
        return null;
    }
}
