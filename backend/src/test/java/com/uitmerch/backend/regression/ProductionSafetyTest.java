package com.uitmerch.backend.regression;

import com.uitmerch.backend.common.config.ProductionSafetyRunListener;
import com.uitmerch.backend.auth.controller.DevOtpController;
import com.uitmerch.backend.common.config.DevDataInitializer;
import com.uitmerch.backend.common.service.DevEmailService;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import static org.assertj.core.api.Assertions.*;

class ProductionSafetyTest {
    private void validate(String[] profiles, Map<String, Object> values) {
        var env = new StandardEnvironment();
        env.setActiveProfiles(profiles);
        var properties = new java.util.HashMap<String,Object>(values);
        properties.putIfAbsent("app.cors.allowed-origins","https://uitmerch.example.test");
        env.getPropertySources().addFirst(new MapPropertySource("test", properties));
        new ProductionSafetyRunListener(null, null).environmentPrepared(null, env);
    }

    @Test void productionAndStagingRejectEveryDevelopmentProfile() {
        for (String deployment : new String[]{"production", "staging"}) {
            for (String profile : new String[]{"dev", "docker"}) {
                assertThatThrownBy(() -> validate(new String[]{profile}, Map.of("app.environment", deployment)))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("dev or docker");
            }
        }
    }

    @Test void productionProfileAndPlatformCannotBeOverriddenByLocalMarker() {
        assertThatThrownBy(() -> validate(new String[]{"production", "dev"}, Map.of("app.environment", "local")))
            .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> validate(new String[]{"docker"}, Map.of("app.environment", "local", "RENDER", "true")))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test void productionRejectsOptInFeaturesAndDestructiveSchemaSettings() {
        for (String flag : new String[]{"otp-endpoint", "seed-data", "mock-mail"}) {
            assertThatThrownBy(() -> validate(new String[]{}, Map.of("app.environment", "production", "app.dev." + flag, true)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining(flag);
        }
        assertThatThrownBy(() -> validate(new String[]{}, Map.of("app.environment", "production", "spring.h2.console.enabled", true)))
            .isInstanceOf(IllegalStateException.class);
        for (String ddl : new String[]{"update", "create", "create-drop"}) {
            assertThatThrownBy(() -> validate(new String[]{}, Map.of("app.environment", "production", "spring.jpa.hibernate.ddl-auto", ddl)))
                .isInstanceOf(IllegalStateException.class);
        }
    }

    @Test void explicitLocalAndTestProfilesRemainAvailable() {
        for (String deployment : new String[]{"local", "test"}) {
            assertThatCode(() -> validate(new String[]{"dev"}, Map.of("app.environment", deployment, "app.dev.otp-endpoint", true)))
                .doesNotThrowAnyException();
        }
    }

    @Test void safeProductionAndUnknownMarkerAreHandledExplicitly() {
        assertThatCode(() -> validate(new String[]{"production"}, Map.of("app.environment", "production")))
            .doesNotThrowAnyException();
        assertThatThrownBy(() -> validate(new String[]{}, Map.of("app.environment", "prodction")))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test void devToolsAreAbsentByDefaultEvenWithDevProfile() {
        new ApplicationContextRunner().withPropertyValues("spring.profiles.active=dev")
            .withUserConfiguration(DevOtpController.class, DevDataInitializer.class, DevEmailService.class)
            .run(context -> assertThat(context).doesNotHaveBean(DevOtpController.class)
                .doesNotHaveBean(DevDataInitializer.class).doesNotHaveBean(DevEmailService.class));
    }

    @Test void factoriesRegisterGuardBeforeApplicationBeansAreCreated() {
        var app = new SpringApplication(EmptyApplication.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        assertThatThrownBy(() -> app.run("--app.environment=production", "--spring.profiles.active=docker",
                "--spring.config.location=optional:classpath:/no-production-test-config.yaml"))
            .hasStackTraceContaining("Deployed environments cannot enable");
    }

    @Test void deployedCheckoutProtectionsCannotBeDisabled() {
        for (String flag : new String[]{"app.rate-limit.shared", "app.checkout.expiry-enabled"})
            assertThatThrownBy(() -> validate(new String[]{}, Map.of("app.environment", "production", flag, false)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test void productionRequiresSecureCookiesAndExactHttpsOrigins() {
        assertThatThrownBy(() -> validate(new String[]{}, Map.of("app.environment", "production", "app.auth.cookie-secure", false))).isInstanceOf(IllegalStateException.class);
        for(String origin:new String[]{"", "*", "http://localhost:5173", "https://example.test/path", "https://example.test?query=1"})
            assertThatThrownBy(() -> validate(new String[]{}, Map.of("app.environment", "production", "app.cors.allowed-origins", origin))).isInstanceOf(IllegalStateException.class);
    }

    @Test void publishedTestSigningKeysCannotBeUsedInProduction() {
        assertThatThrownBy(() -> validate(new String[]{}, Map.of("app.environment", "production", "app.jwt.secret", "uitmerch-disposable-test-suite-secret-2026"))).isInstanceOf(IllegalStateException.class);
    }

    @Configuration(proxyBeanMethods = false)
    static class EmptyApplication {}
}
