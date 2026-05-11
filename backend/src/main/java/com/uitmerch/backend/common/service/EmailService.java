package com.uitmerch.backend.common.service;

public interface EmailService {
    void sendOtp(String toEmail, String otpCode);
}
