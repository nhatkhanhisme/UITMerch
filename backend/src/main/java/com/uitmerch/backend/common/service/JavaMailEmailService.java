package com.uitmerch.backend.common.service;

import com.uitmerch.backend.common.exception.StorageException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Profile("!(dev | docker)")
@RequiredArgsConstructor
public class JavaMailEmailService implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Override
    public void sendOtp(String toEmail, String otpCode) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject("Your UITMerch verification code");
            helper.setText(buildHtml(otpCode), true);

            mailSender.send(message);
            log.info("OTP email sent to {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send OTP email to {}: {}", toEmail, e.getMessage());
            throw new StorageException("Failed to send verification email. Please try again.", e);
        }
    }

    private String buildHtml(String otpCode) {
        return """
            <div style="font-family:Arial,sans-serif;max-width:480px;margin:0 auto;padding:32px;
                        background:#f9f9f9;border-radius:8px;border:1px solid #e0e0e0">
              <h2 style="color:#1a1a1a;margin-bottom:8px">Email Verification</h2>
              <p style="color:#444;margin-bottom:24px">
                Use the code below to verify your UITMerch account.
                It expires in <strong>15 minutes</strong>.
              </p>
              <div style="background:#ffffff;border:1px solid #d0d0d0;border-radius:6px;
                          padding:20px;text-align:center;letter-spacing:8px;
                          font-size:32px;font-weight:bold;color:#1a1a1a">
                %s
              </div>
              <p style="color:#888;font-size:12px;margin-top:24px">
                If you did not request this code, you can safely ignore this email.
              </p>
            </div>
            """.formatted(otpCode);
    }
}
