package com.uitmerch.backend.regression;

import com.uitmerch.backend.common.config.DevDatasourceEnvironmentPostProcessor;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import static org.assertj.core.api.Assertions.*;

class DevDatasourceEnvironmentRegressionTest {
    private StandardEnvironment environment(String profile) {
        var environment = new StandardEnvironment();
        environment.setActiveProfiles(profile);
        environment.getPropertySources().addFirst(new SystemEnvironmentPropertySource("deployedCredentials", Map.of(
                "SPRING_DATASOURCE_URL", "jdbc:postgresql://remote/production",
                "SPRING_DATASOURCE_USERNAME", "production",
                "SPRING_DATASOURCE_PASSWORD", "production-secret",
                "SPRING_DATASOURCE_HIKARI_JDBC_URL", "jdbc:postgresql://remote/production")));
        return environment;
    }

    @Test void exportedProductionCredentialsCannotReachTheDevDatabase() {
        var environment = environment("dev");
        new DevDatasourceEnvironmentPostProcessor().postProcessEnvironment(environment, null);
        assertThat(environment.getProperty("spring.datasource.url")).startsWith("jdbc:h2:mem:");
        assertThat(environment.getProperty("spring.datasource.hikari.jdbc-url")).startsWith("jdbc:h2:mem:");
        assertThat(environment.getProperty("spring.datasource.driver-class-name")).isEqualTo("org.h2.Driver");
        assertThat(environment.getProperty("spring.datasource.username")).isEqualTo("sa");
        assertThat(environment.getProperty("spring.datasource.password")).isEmpty();
    }

    @Test void deployedProfilesRetainTheirEnvironmentCredentials() {
        for (String profile : new String[]{"default", "docker", "prod"}) {
            var environment = environment(profile);
            new DevDatasourceEnvironmentPostProcessor().postProcessEnvironment(environment, null);
            assertThat(environment.getProperty("spring.datasource.url")).isEqualTo("jdbc:postgresql://remote/production");
            assertThat(environment.getProperty("spring.datasource.username")).isEqualTo("production");
            assertThat(environment.getProperty("spring.datasource.password")).isEqualTo("production-secret");
        }
    }

    @Test void customDevDatabaseCannotUseAPersistentOrRemoteUrl() {
        var environment = environment("dev");
        environment.getPropertySources().addFirst(new SystemEnvironmentPropertySource("devOverride",
                Map.of("UITMERCH_DEV_DATASOURCE_URL", "jdbc:postgresql://remote/production")));
        assertThatThrownBy(() -> new DevDatasourceEnvironmentPostProcessor().postProcessEnvironment(environment, null))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("in-memory H2");
    }
}
