package com.uitmerch.backend.auth.controller;

import com.uitmerch.backend.auth.entity.OtpToken;
import com.uitmerch.backend.auth.entity.User;
import com.uitmerch.backend.auth.repository.OtpTokenRepository;
import com.uitmerch.backend.auth.repository.UserRepository;
import com.uitmerch.backend.common.exception.ResourceNotFoundException;
import com.uitmerch.backend.common.model.ApiResponse;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/dev")
@RequiredArgsConstructor
@Profile("dev")
public class DevOtpController {

    private final UserRepository userRepository;
    private final OtpTokenRepository otpTokenRepository;

    @GetMapping("/otps")
    public ResponseEntity<ApiResponse<OtpInfo>> getLatestOtp(@RequestParam @Email String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));

        Optional<OtpToken> otpOpt = otpTokenRepository.findTopByUserOrderByCreatedAtDesc(user);
        if (otpOpt.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success("No OTP found for this user.", null));
        }

        OtpToken otp = otpOpt.get();
        OtpInfo info = new OtpInfo(otp.getOtpCode(), otp.getExpiresAt().toString(), otp.isUsed());
        return ResponseEntity.ok(ApiResponse.success("OTP retrieved.", info));
    }

    @Data
    @AllArgsConstructor
    static class OtpInfo {
        private String code;
        private String expiresAt;
        private boolean used;
    }
}
