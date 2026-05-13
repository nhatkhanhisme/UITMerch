package com.uitmerch.backend.common.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@SecurityScheme(
    name = "bearerAuth",
    scheme = "bearer",
    bearerFormat = "JWT",
    type = SecuritySchemeType.HTTP,
    in = SecuritySchemeIn.HEADER
)
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
            .info(new Info()
                .title("UITMerch API")
                .version("2.0")
                .description("UIT Merchandise Platform — multi-role e-commerce for clubs and faculties."))
            .tags(List.of(
                new Tag().name("Admin").description("Platform governance — users, organizations, orders"),
                new Tag().name("Auth").description("Registration, email verification, and login"),
                new Tag().name("Public").description("Browse merchandise, organizations, events (no auth)"),
                new Tag().name("Organizer").description("Manage organization, merch catalog, orders, and events"),
                new Tag().name("Customer").description("Profile, cart, orders, and wishlist")
            ));
    }
}
