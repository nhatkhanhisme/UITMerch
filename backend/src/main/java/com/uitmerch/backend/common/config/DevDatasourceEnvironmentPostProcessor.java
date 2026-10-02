package com.uitmerch.backend.common.config;

import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.Profiles;

/** Keep the destructive, in-memory dev schema independent of deployed credentials. */
public class DevDatasourceEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!environment.acceptsProfiles(Profiles.of("dev"))) {
            return;
        }
        String url = environment.getProperty("uitmerch.dev.datasource.url",
                "jdbc:h2:mem:uitmerch;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
        if (!url.startsWith("jdbc:h2:mem:")) {
            throw new IllegalStateException("The dev profile requires an in-memory H2 URL; use the default profile for PostgreSQL.");
        }
        environment.getPropertySources().addFirst(new MapPropertySource("uitmerchDevDatasource", Map.of(
                "spring.datasource.url", url,
                "spring.datasource.driver-class-name", "org.h2.Driver",
                "spring.datasource.username", "sa",
                "spring.datasource.password", "",
                "spring.datasource.hikari.jdbc-url", url,
                "spring.datasource.hikari.driver-class-name", "org.h2.Driver",
                "spring.datasource.hikari.username", "sa",
                "spring.datasource.hikari.password", "")));
    }

    @Override
    public int getOrder() {
        return ConfigDataEnvironmentPostProcessor.ORDER + 1;
    }
}
