package com.uitmerch.backend.performance;

import com.uitmerch.backend.auth.dto.LoginRequest;
import com.uitmerch.backend.auth.entity.User;
import com.uitmerch.backend.auth.repository.UserRepository;
import com.uitmerch.backend.auth.service.AuthService;
import com.uitmerch.backend.common.model.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "app.jwt.secret=uitmerch-disposable-observability-test-secret-2026",
    "app.delivery.enabled=false", "app.campaigns.enabled=false"
})
@ActiveProfiles("docker")
@AutoConfigureMockMvc
@AutoConfigureObservability
@EnabledIfEnvironmentVariable(named = "UITMERCH_TEST_DATABASE_URL", matches = "jdbc:postgresql://127\\.0\\.0\\.1:.*")
class ApiObservabilitySecurityTest {
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", () -> System.getenv("UITMERCH_TEST_DATABASE_URL"));
        properties.add("spring.datasource.username", () -> "postgres");
        properties.add("spring.datasource.password", () -> "uitmerch_test_only");
    }
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired AuthService auth;
    @Autowired PasswordEncoder passwords;

    String token(UserRole role) {
        User user = users.save(User.builder().email(UUID.randomUUID() + "@uit.edu.vn")
            .passwordHash(passwords.encode("Password1")).fullName("Metrics Test")
            .role(role).isVerified(true).build());
        LoginRequest login = new LoginRequest(); login.setEmail(user.getEmail()); login.setPassword("Password1");
        return "Bearer " + auth.login(login).getToken();
    }

    @Test
    void onlyAdminsCanReadOperatorMetricsAndPoolMetricsAreRegistered() throws Exception {
        mvc.perform(get("/actuator/metrics")).andExpect(status().isUnauthorized());
        mvc.perform(get("/actuator/metrics").header("Authorization", token(UserRole.CUSTOMER))).andExpect(status().isForbidden());
        mvc.perform(get("/actuator/metrics").header("Authorization", token(UserRole.ORGANIZER))).andExpect(status().isForbidden());
        String admin = token(UserRole.ADMIN);
        mvc.perform(get("/")).andExpect(status().isOk());
        mvc.perform(get("/actuator/metrics").header("Authorization", admin))
            .andExpect(status().isOk()).andExpect(jsonPath("$.names", hasItem("hikaricp.connections.active")))
            .andExpect(jsonPath("$.names", hasItem("http.server.requests")));
    }

    @Test
    void actuatorHealthHasNoDetailsAndOtherEndpointsAreNotExposed() throws Exception {
        String admin = token(UserRole.ADMIN);
        mvc.perform(get("/actuator/health").header("Authorization", admin))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.components").doesNotExist());
        mvc.perform(get("/actuator/env").header("Authorization", admin)).andExpect(status().isNotFound());
    }
}
