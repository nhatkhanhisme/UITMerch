package com.uitmerch.backend.common.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Profile("dev | docker")
public class DevEmailService implements EmailService {

    @Override
    public void sendOtp(String toEmail, String otpCode) {
        log.info("===== [DEV] OTP for {} → {} =====", toEmail, otpCode);
    }
}
