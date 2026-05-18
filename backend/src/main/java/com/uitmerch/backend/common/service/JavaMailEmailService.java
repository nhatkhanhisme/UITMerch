package com.uitmerch.backend.common.service;

import com.uitmerch.backend.common.exception.StorageException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Profile("!(dev | docker)")
@RequiredArgsConstructor
public class JavaMailEmailService implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Value("${app.mail.from-name:UITMerch}")
    private String fromName;

    @Async
    @Override
    public void sendOtp(String toEmail, String otpCode) {
        sendMail(toEmail, "Your UITMerch verification code", buildHtml(otpCode));
    }

    @Async
    @Override
    public void sendPasswordReset(String toEmail, String otpCode) {
        sendMail(toEmail, "Reset your UITMerch password", buildOtpHtml(
            "Password Reset",
            "Use the code below to reset your UITMerch password. It expires in <strong>15 minutes</strong>.",
            otpCode,
            "If you did not request a password reset, you can safely ignore this email."
        ));
    }

    @Async
    @Override
    public void sendOrderStatusUpdate(String toEmail, String orderId, String newStatus) {
        String subject = "Your UITMerch order has been updated";
        String body = """
            <div style="font-family:Arial,sans-serif;max-width:480px;margin:0 auto;padding:32px;
                        background:#f9f9f9;border-radius:8px;border:1px solid #e0e0e0">
              <h2 style="color:#1a1a1a;margin-bottom:8px">Order Update</h2>
              <p style="color:#444;">Your order <strong>%s</strong> status has changed to:</p>
              <div style="background:#ffffff;border:1px solid #d0d0d0;border-radius:6px;
                          padding:20px;text-align:center;font-size:24px;font-weight:bold;color:#1a1a1a">
                %s
              </div>
              <p style="color:#888;font-size:12px;margin-top:24px">
                Thank you for shopping at UITMerch.
              </p>
            </div>
            """.formatted(orderId, newStatus);
        sendMail(toEmail, subject, body);
    }

    @Async
    @Override
    public void sendPickupScheduleNotification(String toEmail, String orderId,
                                               String pickupDate, String pickupTimeSlot,
                                               String location, String notes) {
        String notesHtml = (notes != null && !notes.isBlank())
            ? "<p style=\"color:#444;\"><strong>Ghi chú:</strong> " + notes + "</p>"
            : "";
        String body = """
            <div style="font-family:Arial,sans-serif;max-width:480px;margin:0 auto;padding:32px;
                        background:#f9f9f9;border-radius:8px;border:1px solid #e0e0e0">
              <h2 style="color:#1a1a1a;margin-bottom:8px">Lịch nhận hàng đã được tạo</h2>
              <p style="color:#444;">Đơn hàng <strong>%s</strong> của bạn đã sẵn sàng.
                 Vui lòng đến nhận theo lịch dưới đây:</p>
              <div style="background:#ffffff;border:1px solid #d0d0d0;border-radius:6px;padding:20px;margin:16px 0">
                <p style="margin:4px 0;color:#1a1a1a;"><strong>Ngày:</strong> %s</p>
                <p style="margin:4px 0;color:#1a1a1a;"><strong>Khung giờ:</strong> %s</p>
                <p style="margin:4px 0;color:#1a1a1a;"><strong>Địa điểm:</strong> %s</p>
              </div>
              %s
              <p style="color:#888;font-size:12px;margin-top:24px">
                Vui lòng xuất trình <strong>mã đơn hàng</strong> khi đến nhận.<br>
                Mã đơn: <strong>%s</strong>
              </p>
            </div>
            """.formatted(orderId, pickupDate, pickupTimeSlot, location, notesHtml, orderId);
        sendMail(toEmail, "UITMerch — Lịch nhận hàng của bạn", body);
    }

    @Async
    @Override
    public void sendOrderCancelledNotification(String toEmail, String orderId,
                                               String cancelReason, String cancelledBy) {
        String cancelledByVi = "customer".equalsIgnoreCase(cancelledBy) ? "Khách hàng" : "Ban tổ chức";
        String body = """
            <div style="font-family:Arial,sans-serif;max-width:480px;margin:0 auto;padding:32px;
                        background:#f9f9f9;border-radius:8px;border:1px solid #e0e0e0">
              <h2 style="color:#1a1a1a;margin-bottom:8px">Đơn hàng đã bị huỷ</h2>
              <p style="color:#444;">Đơn hàng <strong>%s</strong> đã bị huỷ bởi <strong>%s</strong>.</p>
              <div style="background:#ffffff;border:1px solid #d0d0d0;border-radius:6px;
                          padding:16px;margin:16px 0;color:#444;">
                <strong>Lý do:</strong> %s
              </div>
              <p style="color:#888;font-size:12px;margin-top:24px">
                Nếu bạn có thắc mắc, vui lòng liên hệ ban tổ chức.<br>
                Cảm ơn bạn đã sử dụng UITMerch.
              </p>
            </div>
            """.formatted(orderId, cancelledByVi, cancelReason);
        sendMail(toEmail, "UITMerch — Đơn hàng đã bị huỷ", body);
    }

    private void sendMail(String toEmail, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(new InternetAddress(fromAddress, fromName));
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("Email '{}' sent to {}", subject, toEmail);
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            log.error("Failed to send email '{}' to {}: {}", subject, toEmail, e.getMessage());
            throw new StorageException("Failed to send email. Please try again.", e);
        }
    }

    private String buildOtpHtml(String title, String description, String otpCode, String footer) {
        return """
            <div style="font-family:Arial,sans-serif;max-width:480px;margin:0 auto;padding:32px;
                        background:#f9f9f9;border-radius:8px;border:1px solid #e0e0e0">
              <h2 style="color:#1a1a1a;margin-bottom:8px">%s</h2>
              <p style="color:#444;margin-bottom:24px">%s</p>
              <div style="background:#ffffff;border:1px solid #d0d0d0;border-radius:6px;
                          padding:20px;text-align:center;letter-spacing:8px;
                          font-size:32px;font-weight:bold;color:#1a1a1a">
                %s
              </div>
              <p style="color:#888;font-size:12px;margin-top:24px">%s</p>
            </div>
            """.formatted(title, description, otpCode, footer);
    }

    private String buildHtml(String otpCode) {
        return buildOtpHtml(
            "Email Verification",
            "Use the code below to verify your UITMerch account. It expires in <strong>15 minutes</strong>.",
            otpCode,
            "If you did not request this code, you can safely ignore this email."
        );
    }
}
