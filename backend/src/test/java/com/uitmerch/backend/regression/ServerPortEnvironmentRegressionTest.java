package com.uitmerch.backend.regression;

import com.uitmerch.backend.common.config.ServerPortApplicationRunListener;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.core.env.SimpleCommandLinePropertySource;
import static org.assertj.core.api.Assertions.*;

class ServerPortEnvironmentRegressionTest {
    private StandardEnvironment environment() {
        var environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new SystemEnvironmentPropertySource("deployedEnvironment",
                Map.of("PORT", "19090", "SERVER_PORT", "18080")));
        return environment;
    }

    @Test void platformPortOverridesTheDirectServerPortEnvironmentBinding() {
        var environment = environment();
        new ServerPortApplicationRunListener(null, new String[0]).environmentPrepared(null, environment);
        assertThat(environment.getProperty("server.port")).isEqualTo("19090");
    }

    @Test void localDotenvPortAlsoOverridesTheServerPortBinding() {
        var environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("dotenv",
                Map.of("PORT", "19090", "SERVER_PORT", "18080")));
        new ServerPortApplicationRunListener(null, new String[0]).environmentPrepared(null, environment);
        assertThat(environment.getProperty("server.port")).isEqualTo("19090");
    }

    @Test void explicitCommandLinePortRetainsPrecedence() {
        var environment = environment();
        environment.getPropertySources().addFirst(new SimpleCommandLinePropertySource("--server.port=0"));
        new ServerPortApplicationRunListener(null, new String[0]).environmentPrepared(null, environment);
        assertThat(environment.getProperty("server.port")).isEqualTo("0");
    }

    @Test void malformedPlatformPortFailsAtStartup() {
        var environment = environment();
        environment.getPropertySources().addFirst(new MapPropertySource("invalidPort", Map.of("PORT", "invalid")));
        assertThatThrownBy(() -> new ServerPortApplicationRunListener(null, new String[0])
                .environmentPrepared(null, environment)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PORT must be an integer");
    }
}
