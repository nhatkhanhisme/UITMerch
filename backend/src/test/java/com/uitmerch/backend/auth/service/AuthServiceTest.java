package com.uitmerch.backend.auth.service;

import com.uitmerch.backend.auth.dto.LoginRequest;
import com.uitmerch.backend.auth.dto.RegisterRequest;
import com.uitmerch.backend.auth.dto.VerifyEmailRequest;
import com.uitmerch.backend.auth.entity.OtpToken;
import com.uitmerch.backend.auth.entity.User;
import com.uitmerch.backend.auth.repository.OtpTokenRepository;
import com.uitmerch.backend.auth.repository.UserRepository;
import com.uitmerch.backend.common.exception.AuthenticationException;
import com.uitmerch.backend.common.exception.InvalidOtpException;
import com.uitmerch.backend.common.exception.UnverifiedEmailException;
import com.uitmerch.backend.common.exception.UserAlreadyExistsException;
import com.uitmerch.backend.common.exception.ValidationException;
import com.uitmerch.backend.common.model.UserRole;
import com.uitmerch.backend.common.security.JwtTokenProvider;
import com.uitmerch.backend.common.service.EmailService;
import com.uitmerch.backend.common.service.TokenBlacklistService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private OtpTokenRepository otpTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private EmailService emailService;
    @Mock private TokenBlacklistService tokenBlacklistService;

    @InjectMocks private AuthService authService;

    // ── register ────────────────────────────────────────────────────────────

    @Test
    void register_success_savesUserAndSendsOtp() {
        when(userRepository.existsByEmail("new@uit.edu.vn")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(otpTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RegisterRequest req = new RegisterRequest();
        req.setEmail("new@uit.edu.vn");
        req.setPassword("Password1");
        req.setFullName("Test User");

        assertThatNoException().isThrownBy(() -> authService.register(req));
        verify(emailService).sendOtp(eq("new@uit.edu.vn"), any());
    }

    @Test
    void register_duplicateEmail_throwsUserAlreadyExists() {
        when(userRepository.existsByEmail("dup@uit.edu.vn")).thenReturn(true);

        RegisterRequest req = new RegisterRequest();
        req.setEmail("dup@uit.edu.vn");
        req.setPassword("Password1");
        req.setFullName("Test User");

        assertThatThrownBy(() -> authService.register(req))
            .isInstanceOf(UserAlreadyExistsException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_passwordTooShort_throwsValidation() {
        when(userRepository.existsByEmail(any())).thenReturn(false);

        RegisterRequest req = new RegisterRequest();
        req.setEmail("test@uit.edu.vn");
        req.setPassword("Pass1"); // 5 chars
        req.setFullName("Test User");

        assertThatThrownBy(() -> authService.register(req))
            .isInstanceOf(ValidationException.class);
    }

    @Test
    void register_passwordNoUppercase_throwsValidation() {
        when(userRepository.existsByEmail(any())).thenReturn(false);

        RegisterRequest req = new RegisterRequest();
        req.setEmail("test@uit.edu.vn");
        req.setPassword("password1"); // no uppercase
        req.setFullName("Test User");

        assertThatThrownBy(() -> authService.register(req))
            .isInstanceOf(ValidationException.class);
    }

    @Test
    void register_passwordNoDigit_throwsValidation() {
        when(userRepository.existsByEmail(any())).thenReturn(false);

        RegisterRequest req = new RegisterRequest();
        req.setEmail("test@uit.edu.vn");
        req.setPassword("Password"); // no digit
        req.setFullName("Test User");

        assertThatThrownBy(() -> authService.register(req))
            .isInstanceOf(ValidationException.class);
    }

    @Test
    void register_passwordNoLowercase_throwsValidation() {
        when(userRepository.existsByEmail(any())).thenReturn(false);

        RegisterRequest req = new RegisterRequest();
        req.setEmail("test@uit.edu.vn");
        req.setPassword("PASSWORD1"); // no lowercase
        req.setFullName("Test User");

        assertThatThrownBy(() -> authService.register(req))
            .isInstanceOf(ValidationException.class);
    }

    // ── verifyEmail ─────────────────────────────────────────────────────────

    @Test
    void verifyEmail_correctCode_verifiesUser() {
        User user = User.builder().id(UUID.randomUUID()).email("test@uit.edu.vn").build();
        OtpToken otp = OtpToken.builder()
            .otpCode("123456")
            .expiresAt(LocalDateTime.now().plusMinutes(10))
            .isUsed(false)
            .attemptCount(0)
            .build();

        when(userRepository.findByEmail("test@uit.edu.vn")).thenReturn(Optional.of(user));
        when(otpTokenRepository.findTopByUserAndIsUsedFalseOrderByCreatedAtDesc(user)).thenReturn(Optional.of(otp));
        when(otpTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        VerifyEmailRequest req = new VerifyEmailRequest();
        req.setEmail("test@uit.edu.vn");
        req.setOtpCode("123456");

        assertThatNoException().isThrownBy(() -> authService.verifyEmail(req));
        assertThat(otp.isUsed()).isTrue();
        assertThat(user.isVerified()).isTrue();
    }

    @Test
    void verifyEmail_wrongCode_incrementsAttemptCount() {
        User user = User.builder().id(UUID.randomUUID()).email("test@uit.edu.vn").build();
        OtpToken otp = OtpToken.builder()
            .otpCode("123456")
            .expiresAt(LocalDateTime.now().plusMinutes(10))
            .isUsed(false)
            .attemptCount(0)
            .build();

        when(userRepository.findByEmail("test@uit.edu.vn")).thenReturn(Optional.of(user));
        when(otpTokenRepository.findTopByUserAndIsUsedFalseOrderByCreatedAtDesc(user)).thenReturn(Optional.of(otp));
        when(otpTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        VerifyEmailRequest req = new VerifyEmailRequest();
        req.setEmail("test@uit.edu.vn");
        req.setOtpCode("WRONG!");

        assertThatThrownBy(() -> authService.verifyEmail(req))
            .isInstanceOf(InvalidOtpException.class);
        assertThat(otp.getAttemptCount()).isEqualTo(1);
        assertThat(otp.isUsed()).isFalse();
    }

    @Test
    void verifyEmail_expiredOtp_throws() {
        User user = User.builder().id(UUID.randomUUID()).email("test@uit.edu.vn").build();
        OtpToken otp = OtpToken.builder()
            .otpCode("123456")
            .expiresAt(LocalDateTime.now().minusMinutes(1))
            .isUsed(false)
            .build();

        when(userRepository.findByEmail("test@uit.edu.vn")).thenReturn(Optional.of(user));
        when(otpTokenRepository.findTopByUserAndIsUsedFalseOrderByCreatedAtDesc(user)).thenReturn(Optional.of(otp));

        VerifyEmailRequest req = new VerifyEmailRequest();
        req.setEmail("test@uit.edu.vn");
        req.setOtpCode("123456");

        assertThatThrownBy(() -> authService.verifyEmail(req))
            .isInstanceOf(InvalidOtpException.class);
    }

    @Test
    void verifyEmail_activeLock_throwsWithMinutesRemaining() {
        User user = User.builder().id(UUID.randomUUID()).email("test@uit.edu.vn").build();
        OtpToken otp = OtpToken.builder()
            .otpCode("123456")
            .expiresAt(LocalDateTime.now().plusMinutes(10))
            .isUsed(false)
            .attemptCount(5)
            .lockedUntil(LocalDateTime.now().plusMinutes(12))
            .build();

        when(userRepository.findByEmail("test@uit.edu.vn")).thenReturn(Optional.of(user));
        when(otpTokenRepository.findTopByUserAndIsUsedFalseOrderByCreatedAtDesc(user)).thenReturn(Optional.of(otp));

        VerifyEmailRequest req = new VerifyEmailRequest();
        req.setEmail("test@uit.edu.vn");
        req.setOtpCode("123456");

        assertThatThrownBy(() -> authService.verifyEmail(req))
            .isInstanceOf(InvalidOtpException.class)
            .hasMessageContaining("Too many failed attempts");
    }

    @Test
    void verifyEmail_fifthWrongAttempt_triggersLock() {
        User user = User.builder().id(UUID.randomUUID()).email("test@uit.edu.vn").build();
        OtpToken otp = OtpToken.builder()
            .otpCode("123456")
            .expiresAt(LocalDateTime.now().plusMinutes(10))
            .isUsed(false)
            .attemptCount(4) // 5th attempt will hit limit
            .build();

        when(userRepository.findByEmail("test@uit.edu.vn")).thenReturn(Optional.of(user));
        when(otpTokenRepository.findTopByUserAndIsUsedFalseOrderByCreatedAtDesc(user)).thenReturn(Optional.of(otp));
        when(otpTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        VerifyEmailRequest req = new VerifyEmailRequest();
        req.setEmail("test@uit.edu.vn");
        req.setOtpCode("WRONG!");

        assertThatThrownBy(() -> authService.verifyEmail(req))
            .isInstanceOf(InvalidOtpException.class);
        assertThat(otp.getAttemptCount()).isEqualTo(5);
        assertThat(otp.getLockedUntil()).isNotNull();
        assertThat(otp.getLockedUntil()).isAfter(LocalDateTime.now());
    }

    @Test
    void verifyEmail_unknownEmail_throws() {
        when(userRepository.findByEmail("ghost@uit.edu.vn")).thenReturn(Optional.empty());

        VerifyEmailRequest req = new VerifyEmailRequest();
        req.setEmail("ghost@uit.edu.vn");
        req.setOtpCode("123456");

        assertThatThrownBy(() -> authService.verifyEmail(req))
            .isInstanceOf(InvalidOtpException.class);
    }

    // ── login ───────────────────────────────────────────────────────────────

    @Test
    void login_validCredentials_returnsAuthResponse() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
            .id(userId)
            .email("user@uit.edu.vn")
            .passwordHash("hashed")
            .fullName("User One")
            .role(UserRole.CUSTOMER)
            .isVerified(true)
            .build();

        when(userRepository.findByEmail("user@uit.edu.vn")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password1", "hashed")).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(any(), any(), any())).thenReturn("access.token.xyz");
        when(jwtTokenProvider.generateRefreshToken(any())).thenReturn("refresh.token.xyz");

        LoginRequest req = new LoginRequest();
        req.setEmail("user@uit.edu.vn");
        req.setPassword("Password1");

        var response = authService.login(req);

        assertThat(response.getToken()).isEqualTo("access.token.xyz");
        assertThat(response.getRefreshToken()).isEqualTo("refresh.token.xyz");
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getRole()).isEqualTo(UserRole.CUSTOMER);
    }

    @Test
    void login_wrongPassword_throwsAuthentication() {
        User user = User.builder()
            .email("user@uit.edu.vn")
            .passwordHash("hashed")
            .isVerified(true)
            .build();

        when(userRepository.findByEmail("user@uit.edu.vn")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        LoginRequest req = new LoginRequest();
        req.setEmail("user@uit.edu.vn");
        req.setPassword("wrong");

        assertThatThrownBy(() -> authService.login(req))
            .isInstanceOf(AuthenticationException.class);
    }

    @Test
    void login_unverifiedUser_throwsUnverified() {
        User user = User.builder()
            .email("user@uit.edu.vn")
            .passwordHash("hashed")
            .isVerified(false)
            .build();

        when(userRepository.findByEmail("user@uit.edu.vn")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(any(), any())).thenReturn(true);

        LoginRequest req = new LoginRequest();
        req.setEmail("user@uit.edu.vn");
        req.setPassword("Password1");

        assertThatThrownBy(() -> authService.login(req))
            .isInstanceOf(UnverifiedEmailException.class);
    }

    @Test
    void login_unknownEmail_throwsAuthentication() {
        when(userRepository.findByEmail("ghost@uit.edu.vn")).thenReturn(Optional.empty());

        LoginRequest req = new LoginRequest();
        req.setEmail("ghost@uit.edu.vn");
        req.setPassword("Password1");

        assertThatThrownBy(() -> authService.login(req))
            .isInstanceOf(AuthenticationException.class);
    }
}
