package com.uitmerch.backend.auth.service;

import com.uitmerch.backend.auth.dto.AuthResponse;
import com.uitmerch.backend.auth.dto.AuthUserSummary;
import com.uitmerch.backend.auth.dto.LoginRequest;
import com.uitmerch.backend.auth.dto.MessageResponse;
import com.uitmerch.backend.auth.dto.RegisterRequest;
import com.uitmerch.backend.auth.dto.VerifyEmailRequest;
import com.uitmerch.backend.auth.entity.EmailVerification;
import com.uitmerch.backend.auth.entity.User;
import com.uitmerch.backend.auth.repository.EmailVerificationRepository;
import com.uitmerch.backend.auth.repository.UserRepository;
import com.uitmerch.backend.common.exception.AuthenticationException;
import com.uitmerch.backend.common.exception.InvalidOtpException;
import com.uitmerch.backend.common.exception.UnverifiedEmailException;
import com.uitmerch.backend.common.exception.UserAlreadyExistsException;
import com.uitmerch.backend.common.model.UserRole;
import com.uitmerch.backend.common.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.Random;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final int OTP_EXPIRY_MINUTES = 15;

    private final UserRepository userRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthServiceImpl(
        UserRepository userRepository,
        EmailVerificationRepository emailVerificationRepository,
        PasswordEncoder passwordEncoder,
        JwtTokenProvider jwtTokenProvider
    ) {
        this.userRepository = userRepository;
        this.emailVerificationRepository = emailVerificationRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    @Transactional
    public MessageResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (userRepository.findByEmail(email).isPresent()) {
            throw new UserAlreadyExistsException("Email is already registered");
        }

        User user = User.builder()
            .email(email)
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .fullName(request.getFullName().trim())
            .role(UserRole.CUSTOMER)
            .isEmailVerified(false)
            .build();

        User savedUser = userRepository.save(user);

        emailVerificationRepository.deleteByUser_EmailIgnoreCase(email);

        String otpCode = generateOtp();
        EmailVerification emailVerification = EmailVerification.builder()
            .user(savedUser)
            .otpCode(otpCode)
            .expiresAt(Instant.now().plusSeconds(OTP_EXPIRY_MINUTES * 60L))
            .build();
        emailVerificationRepository.save(emailVerification);

        log.info("Email verification OTP for {}: {}", email, otpCode);

        return MessageResponse.builder()
            .message("Registration successful. Please verify your email with the OTP sent.")
            .build();
    }

    @Override
    @Transactional
    public MessageResponse registerOrganizer(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (userRepository.findByEmail(email).isPresent()) {
            throw new UserAlreadyExistsException("Email is already registered");
        }

        User user = User.builder()
            .email(email)
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .fullName(request.getFullName().trim())
            .role(UserRole.ORGANIZER)
            .isEmailVerified(false)
            .build();

        User savedUser = userRepository.save(user);

        emailVerificationRepository.deleteByUser_EmailIgnoreCase(email);

        String otpCode = generateOtp();
        EmailVerification emailVerification = EmailVerification.builder()
            .user(savedUser)
            .otpCode(otpCode)
            .expiresAt(Instant.now().plusSeconds(OTP_EXPIRY_MINUTES * 60L))
            .build();
        emailVerificationRepository.save(emailVerification);

        log.info("Email verification OTP for organizer {}: {}", email, otpCode);

        return MessageResponse.builder()
            .message("Organizer registration successful. Please verify your email with the OTP sent.")
            .build();
    }

    @Override
    @Transactional
    public MessageResponse verifyEmail(VerifyEmailRequest request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        EmailVerification emailVerification = emailVerificationRepository.findByUser_EmailIgnoreCase(email)
            .orElseThrow(() -> new InvalidOtpException("Invalid or expired OTP"));

        if (!emailVerification.getOtpCode().equals(request.getOtpCode())) {
            throw new InvalidOtpException("Invalid or expired OTP");
        }

        if (emailVerification.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidOtpException("Invalid or expired OTP");
        }

        User user = emailVerification.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);
        emailVerificationRepository.delete(emailVerification);

        return MessageResponse.builder()
            .message("Email verified successfully")
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new AuthenticationException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AuthenticationException("Invalid email or password");
        }

        if (!user.isEmailVerified()) {
            throw new UnverifiedEmailException("Email is not verified");
        }

        String accessToken = jwtTokenProvider.generateAccessToken(
            user.getId().toString(),
            user.getEmail(),
            user.getRole().name()
        );
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId().toString());

        return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .user(AuthUserSummary.builder()
                .userId(user.getId().toString())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .isEmailVerified(user.isEmailVerified())
                .build())
            .build();
    }

    private String generateOtp() {
        return String.format("%06d", new Random().nextInt(1_000_000));
    }
}
