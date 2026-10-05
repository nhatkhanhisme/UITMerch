package com.uitmerch.backend.common.config;

import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security Configuration.
 * 
 * Defines:
 * - Public endpoints (auth, catalog browse)
 * - Protected endpoints requiring authentication
 * - Role-based access control (CUSTOMER, ORGANIZER, ADMIN)
 * - JWT filter chain setup
 * 
 * NFR01: JWT access tokens utilized for session management.
 * NFR02: Strict role checks at Controller/Route level.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Autowired
    private Environment environment;

    @Value("${app.cors.allowed-origins:https://uitmerch.vercel.app, http://localhost:3000,http://localhost:5173}")
    private String allowedOrigins;

    @Value("${springdoc.swagger-ui.enabled:false}")
    private boolean swaggerEnabled;

    public SecurityConfig(
        JwtAuthenticationFilter jwtAuthenticationFilter,
        JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
    }
    
    /**
     * Password encoder using BCrypt.
     * BR02: Passwords hashed via BCrypt/Argon2.
     * @return BCryptPasswordEncoder bean
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.stream(allowedOrigins.split(",")).map(String::trim).filter(origin -> !origin.isEmpty()).toList());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Content-Type", "Authorization", "X-Requested-With", "X-Guest-Tracking", "X-CSRF-TOKEN"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
    
    /**
     * Main security filter chain configuration.
     * 
     * PUBLIC ENDPOINTS:
     * - POST /api/v1/auth/register (FR01)
     * - POST /api/v1/auth/login (FR02)
     * - GET /api/v1/public/merch (FR03)
     * - GET /swagger-ui/** (documentation)
     * - GET /v3/api-docs/** (OpenAPI)
     * 
     * PROTECTED ENDPOINTS:
     * - /api/v1/customer/** (requires ROLE_CUSTOMER)
     * - /api/v1/organizer/** (requires ROLE_ORGANIZER)
     * - /api/v1/admin/** (requires ROLE_ADMIN)
     * 
     * @param http HTTP security builder
     * @return SecurityFilterChain bean
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Use stateless session (no session cookies for REST API)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // CSRF disabled for stateless API
            .csrf(csrf -> csrf.disable())

            // Enable CORS for local frontend development
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            .headers(headers -> headers
                .referrerPolicy(policy -> policy.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                .contentSecurityPolicy(policy -> policy.policyDirectives("default-src 'none'; frame-ancestors 'none'; base-uri 'none'; object-src 'none'").reportOnly()))

            // Define authorization rules
            .authorizeHttpRequests(authz -> {
                // Let the servlet container render errors without replacing the
                // original response with an authentication failure.
                authz.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                    .requestMatchers("/").permitAll()
                    .requestMatchers("/api/v1/auth/**").permitAll()
                    .requestMatchers("/api/v1/public/**").permitAll()
                    .requestMatchers("/api/v1/categories/**").permitAll()
                    // Actuator is framework-owned and has no app controller on
                    // which to put @PreAuthorize. Keep operator data ADMIN-only.
                    .requestMatchers("/actuator/**").hasRole("ADMIN");

                // Swagger UI — only opened when explicitly enabled (SWAGGER_ENABLED=true)
                if (swaggerEnabled) {
                    authz.requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll();
                }

                // Dev OTP endpoint — only opened on dev or docker profiles
                if (environment.acceptsProfiles(Profiles.of("dev", "docker"))
                        && environment.getProperty("app.dev.otp-endpoint", Boolean.class, false)) {
                    authz.requestMatchers("/api/v1/dev/**").permitAll();
                }

                authz.anyRequest().authenticated();
            })
            
            // Exception handling
            .exceptionHandling(exceptionHandling ->
                exceptionHandling.authenticationEntryPoint(jwtAuthenticationEntryPoint)
            )
            
            // Add JWT filter before Spring Security's UsernamePasswordAuthenticationFilter
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}
