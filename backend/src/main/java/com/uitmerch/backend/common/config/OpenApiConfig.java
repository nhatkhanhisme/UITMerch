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
            // Tag order follows SRS NFR07: Auth → Public → Customer → Organizer → Admin
            .tags(List.of(
                new Tag().name("Auth").description("Registration, email verification, and login"),
                new Tag().name("Public — Merch").description("Browse published merchandise (no auth)"),
                new Tag().name("Public — Organizations").description("Browse active organizations and their merch (no auth)"),
                new Tag().name("Public — Events").description("Browse published events (no auth)"),
                new Tag().name("Public — Orders").description("Guest checkout (no auth)"),
                new Tag().name("Customer — Profile").description("View and update own profile"),
                new Tag().name("Customer — Cart").description("Cart management and checkout"),
                new Tag().name("Customer — Orders").description("Order history"),
                new Tag().name("Customer — Wishlist").description("Save and manage favourite merch items"),
                new Tag().name("Organizer — Organization").description("Manage own organization profile"),
                new Tag().name("Organizer — Merch").description("Manage merch catalog"),
                new Tag().name("Organizer — Orders").description("View and process incoming orders"),
                new Tag().name("Organizer — Events").description("Create and manage events"),
                new Tag().name("Admin").description("Platform governance — users, organizations, orders")
            ));
    }
}
