package com.uitmerch.backend.common.config;

import java.util.Map;
import org.springframework.boot.ConfigurableBootstrapContext;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/** Resolve the platform port after spring-dotenv has loaded the local environment file. */
public class ServerPortApplicationRunListener implements SpringApplicationRunListener, Ordered {
    public ServerPortApplicationRunListener(SpringApplication application, String[] args) {
    }

    @Override
    public void environmentPrepared(ConfigurableBootstrapContext bootstrapContext, ConfigurableEnvironment environment) {
        var commandLine = environment.getPropertySources().get("commandLineArgs");
        var systemProperties = environment.getPropertySources().get("systemProperties");
        if ((commandLine != null && commandLine.getProperty("server.port") != null)
                || (systemProperties != null && systemProperties.getProperty("server.port") != null)) {
            return;
        }
        String platformPort = environment.getProperty("PORT");
        if (platformPort == null || platformPort.isBlank()) {
            return;
        }
        int port;
        try {
            port = Integer.parseInt(platformPort);
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("PORT must be an integer between 0 and 65535.");
        }
        if (port < 0 || port > 65535) {
            throw new IllegalStateException("PORT must be an integer between 0 and 65535.");
        }
        environment.getPropertySources().addFirst(new MapPropertySource("uitmerchPlatformPort",
                Map.of("server.port", port)));
    }

    @Override
    public int getOrder() {
        // spring-dotenv's run listener uses the default order (0).
        return 1;
    }
}
