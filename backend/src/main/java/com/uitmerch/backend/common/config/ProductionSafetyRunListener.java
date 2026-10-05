package com.uitmerch.backend.common.config;

import java.util.Set;
import org.springframework.boot.ConfigurableBootstrapContext;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Profiles;

/** Fail closed after config files and spring-dotenv have been loaded, before bean creation. */
public class ProductionSafetyRunListener implements SpringApplicationRunListener, Ordered {
    public ProductionSafetyRunListener(SpringApplication application, String[] args) {}

    @Override
    public void environmentPrepared(ConfigurableBootstrapContext bootstrap, ConfigurableEnvironment environment) {
        String deployment = environment.getProperty("app.environment", "production");
        if (!Set.of("local", "test", "staging", "production").contains(deployment)) {
            throw new IllegalStateException("app.environment must be local, test, staging or production.");
        }
        boolean deployed = Set.of("staging", "production").contains(deployment)
            || environment.acceptsProfiles(Profiles.of("prod", "production"))
            || environment.getProperty("RENDER", Boolean.class, false);
        if (!deployed) return;
        String signingKey=environment.getProperty("app.jwt.secret", "");
        if(signingKey.startsWith("uitmerch-disposable-") || signingKey.startsWith("test-secret") || signingKey.startsWith("dev-secret"))
            throw new IllegalStateException("Deployed environments cannot reuse published test signing keys.");
        if (!environment.getProperty("app.auth.cookie-secure", Boolean.class, true)) {
            throw new IllegalStateException("Deployed auth cookies must be Secure.");
        }
        if (!environment.getProperty("app.rate-limit.shared", Boolean.class, true)) {
            throw new IllegalStateException("Deployed environments require shared rate limiting.");
        }
        if (!environment.getProperty("app.checkout.expiry-enabled", Boolean.class, true)) {
            throw new IllegalStateException("Deployed environments require pending checkout expiration.");
        }
        if (environment.acceptsProfiles(Profiles.of("dev", "docker"))) {
            throw new IllegalStateException("Deployed environments cannot enable the dev or docker Spring profiles.");
        }
        for (String feature : Set.of("otp-endpoint", "seed-data", "mock-mail")) {
            if (environment.getProperty("app.dev." + feature, Boolean.class, false)) {
                throw new IllegalStateException("Deployed environments cannot enable app.dev." + feature + ".");
            }
        }
        if (environment.getProperty("spring.h2.console.enabled", Boolean.class, false)) {
            throw new IllegalStateException("Deployed environments cannot enable the H2 console.");
        }
        if (Set.of("create", "create-drop", "update").contains(
                environment.getProperty("spring.jpa.hibernate.ddl-auto", "none"))) {
            throw new IllegalStateException("Deployed schema changes must use migrations.");
        }
        String origins=environment.getProperty("app.cors.allowed-origins", "");
        if(origins.isBlank()) throw new IllegalStateException("Deployed environments require explicit HTTPS frontend origins.");
        for(String value:origins.split(",")) {
            try {
                java.net.URI origin=java.net.URI.create(value.trim());
                if(!"https".equals(origin.getScheme()) || origin.getHost()==null || origin.getUserInfo()!=null
                    || (origin.getRawPath()!=null && !origin.getRawPath().isEmpty()) || origin.getQuery()!=null || origin.getFragment()!=null)
                    throw new IllegalArgumentException();
            } catch(IllegalArgumentException ex) {throw new IllegalStateException("Deployed CORS origins must be exact HTTPS origins without paths or wildcards.");}
        }
    }

    @Override public int getOrder() { return 2; }
}
