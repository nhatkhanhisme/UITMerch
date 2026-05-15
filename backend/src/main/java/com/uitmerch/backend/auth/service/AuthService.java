package com.uitmerch.backend.auth.service;

import com.uitmerch.backend.auth.dto.*;
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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int OTP_EXPIRY_MINUTES = 15;
    private static final int OTP_MAX_ATTEMPTS   = 5;
    private static final int OTP_LOCK_MINUTES   = 15;

    private final UserRepository userRepository;
    private final OtpTokenRepository otpTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService;
    private final TokenBlacklistService tokenBlacklistService;

    @Transactional
    public void register(RegisterRequest request) {
        registerWithRole(
                request.getEmail(),
                request.getPassword(),
                request.getFullName(),
                request.getPhone(),
                request.getAddress(),
                UserRole.CUSTOMER
        );
    }

    @Transactional
    public void registerOrganizer(RegisterOrganizerRequest request) {
        registerWithRole(
                request.getEmail(),
                request.getPassword(),
                request.getFullName(),
                request.getPhone(),
                request.getAddress(),
                UserRole.ORGANIZER
        );
    }

    @Transactional
    public void verifyEmail(VerifyEmailRequest request) {
        // Generic error for both unknown email and wrong code — prevents user enumeration
        final InvalidOtpException genericError = new InvalidOtpException("Invalid email or OTP code");

        User user = userRepository.findByEmail(request.getEmail()).orElseThrow(() -> genericError);

        OtpToken otp = otpTokenRepository
                .findTopByUserAndIsUsedFalseOrderByCreatedAtDesc(user)
                .orElseThrow(() -> genericError);

        // Enforce lock-out after repeated failures
        if (otp.getLockedUntil() != null && otp.getLockedUntil().isAfter(LocalDateTime.now())) {
            long minutesLeft = ChronoUnit.MINUTES.between(LocalDateTime.now(), otp.getLockedUntil()) + 1;
            throw new InvalidOtpException(
                    "Too many failed attempts. Please try again in " + minutesLeft + " minute(s).");
        }

        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw genericError;
        }

        if (!otp.getOtpCode().equals(request.getOtpCode())) {
            otp.setAttemptCount(otp.getAttemptCount() + 1);
            if (otp.getAttemptCount() >= OTP_MAX_ATTEMPTS) {
                otp.setLockedUntil(LocalDateTime.now().plusMinutes(OTP_LOCK_MINUTES));
                log.warn("OTP locked for user {} after {} failed attempts", user.getEmail(), OTP_MAX_ATTEMPTS);
            }
            otpTokenRepository.save(otp);
            throw genericError;
        }

        otp.setUsed(true);
        otpTokenRepository.save(otp);

        user.setVerified(true);
        userRepository.save(user);

        log.info("Email verified for user: {}", user.getEmail());
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthenticationException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AuthenticationException("Invalid email or password");
        }

        if (!user.isActive()) {
            throw new AuthenticationException("This account has been deactivated. Please contact support.");
        }

        if (!user.isVerified()) {
            throw new UnverifiedEmailException("Email is not verified yet. Please check your email for the OTP.");
        }

        String accessToken = jwtTokenProvider.generateAccessToken(
            user.getId().toString(), user.getEmail(), user.getRole().name()
        );

        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId().toString());

        return AuthResponse.builder()
            .token(accessToken)
            .tokenType("Bearer")
            .refreshToken(refreshToken)
            .userId(user.getId())
            .email(user.getEmail())
            .fullName(user.getFullName())
            .role(user.getRole())
            .isVerified(user.isVerified())
            .build();
    }

    public AuthResponse refreshToken(String refreshToken) {
        if (refreshToken == null
                || !jwtTokenProvider.validateAsRefreshToken(refreshToken)
                || tokenBlacklistService.isBlacklisted(refreshToken)) {
            throw new AuthenticationException("Invalid or expired refresh token");
        }

        String userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(java.util.UUID.fromString(userId))
                .orElseThrow(() -> new AuthenticationException("Invalid or expired refresh token"));

        // Rotate: blacklist the used refresh token
        tokenBlacklistService.add(refreshToken, jwtTokenProvider.getExpiryFromToken(refreshToken));

        String newAccessToken = jwtTokenProvider.generateAccessToken(
                user.getId().toString(), user.getEmail(), user.getRole().name());
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getId().toString());

        return AuthResponse.builder()
                .token(newAccessToken)
                .tokenType("Bearer")
                .refreshToken(newRefreshToken)
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .isVerified(user.isVerified())
                .build();
    }

    public void logout(String token) {
        if (token != null && jwtTokenProvider.validateToken(token)) {
            tokenBlacklistService.add(token, jwtTokenProvider.getExpiryFromToken(token));
        }
    }

    private void registerWithRole(
            String email, String password, String fullName,
            String phone, String address, UserRole role
    ) {
        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException("Email is already registered");
        }

        validatePassword(password);

        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .fullName(fullName)
                .phone(phone)
                .address(address)
                .role(role)
                .isVerified(false)
                .build();

        userRepository.save(user);

        issueOtp(user);
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new ValidationException(
                    "Password must be at least 8 characters and include uppercase, lowercase, and a number"
            );
        }
        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        if (!hasUpper || !hasLower || !hasDigit) {
            throw new ValidationException(
                    "Password must be at least 8 characters and include uppercase, lowercase, and a number"
            );
        }
    }

    private void issueOtp(User user) {
        otpTokenRepository.deleteAllByUser(user);

        String code = generateOtpCode();

        OtpToken otp = OtpToken.builder()
                .user(user)
                .otpCode(code)
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                .isUsed(false)
                .build();

        otpTokenRepository.save(otp);

        emailService.sendOtp(user.getEmail(), code);
    }

    private String generateOtpCode() {
        SecureRandom random = new SecureRandom();
        int code = 100_000 + random.nextInt(900_000);
        return String.valueOf(code);
    }

    @Transactional
    public void resendOtp(String email) {
        // Silent no-op for unknown, already-verified, or inactive accounts — prevents enumeration
        userRepository.findByEmail(email).ifPresent(user -> {
            if (user.isActive() && !user.isVerified()) {
                issueOtp(user);
                log.info("OTP re-issued for unverified user {}", email);
            }
        });
    }

    @Transactional
    public void forgotPassword(String email) {
        // Use a generic response to avoid revealing whether an email is registered
        userRepository.findByEmail(email).ifPresent(user -> {
            if (user.isActive() && user.isVerified()) {
                otpTokenRepository.deleteAllByUser(user);
                String code = generateOtpCode();
                OtpToken otp = OtpToken.builder()
                    .user(user)
                    .otpCode(code)
                    .expiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                    .isUsed(false)
                    .build();
                otpTokenRepository.save(otp);
                emailService.sendPasswordReset(user.getEmail(), code);
                log.info("Password-reset OTP issued for {}", email);
            }
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        final InvalidOtpException genericError = new InvalidOtpException("Invalid email or OTP code");

        User user = userRepository.findByEmail(request.getEmail()).orElseThrow(() -> genericError);

        OtpToken otp = otpTokenRepository
            .findTopByUserAndIsUsedFalseOrderByCreatedAtDesc(user)
            .orElseThrow(() -> genericError);

        if (otp.getLockedUntil() != null && otp.getLockedUntil().isAfter(LocalDateTime.now())) {
            long minutesLeft = ChronoUnit.MINUTES.between(LocalDateTime.now(), otp.getLockedUntil()) + 1;
            throw new InvalidOtpException("Too many failed attempts. Please try again in " + minutesLeft + " minute(s).");
        }

        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw genericError;
        }

        if (!otp.getOtpCode().equals(request.getOtpCode())) {
            otp.setAttemptCount(otp.getAttemptCount() + 1);
            if (otp.getAttemptCount() >= OTP_MAX_ATTEMPTS) {
                otp.setLockedUntil(LocalDateTime.now().plusMinutes(OTP_LOCK_MINUTES));
            }
            otpTokenRepository.save(otp);
            throw genericError;
        }

        validatePassword(request.getNewPassword());

        otp.setUsed(true);
        otpTokenRepository.save(otp);

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("Password reset successfully for {}", user.getEmail());
    }

    @Transactional
    @Scheduled(cron = "0 0 * * * *")
    public void purgeExpiredOtps() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(1);
        otpTokenRepository.deleteExpiredBefore(cutoff);
        log.debug("Purged OTP tokens expired before {}", cutoff);
    }
}
