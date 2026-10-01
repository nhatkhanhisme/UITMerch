package com.uitmerch.backend.regression;

import com.uitmerch.backend.auth.entity.User;
import com.uitmerch.backend.auth.repository.UserRepository;
import com.uitmerch.backend.auth.service.AuthService;
import com.uitmerch.backend.auth.dto.LoginRequest;
import com.uitmerch.backend.common.model.UserRole;
import com.uitmerch.backend.common.exception.AuthenticationException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = "app.delivery.enabled=false")
@ActiveProfiles("dev")
class DevAuthRegressionTest {
    @Autowired AuthService auth;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwords;
    @Test void loginRotationAndLogoutAlsoWorkWithTheDevDatabase() {
        var user = users.save(User.builder().email(UUID.randomUUID() + "@uit.edu.vn").fullName("Dev regression")
            .passwordHash(passwords.encode("Password1")).role(UserRole.CUSTOMER).isVerified(true).build());
        var request = new LoginRequest(); request.setEmail(user.getEmail()); request.setPassword("Password1");
        var login = auth.login(request);
        var rotated = auth.refreshToken(login.getRefreshToken());
        assertThat(rotated.getRefreshToken()).isNotEqualTo(login.getRefreshToken());
        auth.logout(rotated.getToken());
        assertThatThrownBy(() -> auth.refreshToken(rotated.getRefreshToken())).isInstanceOf(AuthenticationException.class);
    }
}
