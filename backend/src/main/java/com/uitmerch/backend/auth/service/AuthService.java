package com.uitmerch.backend.auth.service;

import com.uitmerch.backend.auth.dto.*;
import com.uitmerch.backend.auth.entity.OtpToken;
import com.uitmerch.backend.auth.entity.User;
import com.uitmerch.backend.auth.repository.OtpTokenRepository;
import com.uitmerch.backend.auth.repository.UserRepository;
import com.uitmerch.backend.common.exception.AuthenticationException;
import com.uitmerch.backend.common.exception.InvalidOtpException;
import com.uitmerch.backend.common.exception.ResourceNotFoundException;
import com.uitmerch.backend.common.exception.UnverifiedEmailException;
import com.uitmerch.backend.common.exception.UserAlreadyExistsException;
import com.uitmerch.backend.common.exception.ValidationException;
import com.uitmerch.backend.common.security.JwtTokenProvider;
import com.uitmerch.backend.common.model.UserRole;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int OTP_EXPIRY_MINUTES = 15;

    private final UserRepository userRepository;
    private final OtpTokenRepository otpTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

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
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.getEmail()));

        OtpToken otpToken = otpTokenRepository
                .findTopByUserAndOtpCodeAndIsUsedFalseOrderByCreatedAtDesc(user, request.getOtpCode())
                .orElseThrow(() -> new InvalidOtpException("OTP code is invalid or expired"));

        if (otpToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidOtpException("OTP code has expired");
        }

        otpToken.setUsed(true);
        otpTokenRepository.save(otpToken);

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

        // TODO: send via email (SendGrid / JavaMailSender)
    }

    private String generateOtpCode() {
        SecureRandom random = new SecureRandom();
        int code = 100_000 + random.nextInt(900_000);
        return String.valueOf(code);
    }
}
