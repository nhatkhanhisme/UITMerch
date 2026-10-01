package com.uitmerch.backend.common.delivery;

import com.uitmerch.backend.common.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
@RequiredArgsConstructor
public class OutboxEmailService implements EmailService {
    private final BackgroundJobService jobs;
    @Override public void sendOtp(String email, String code) { jobs.enqueueEmail("OTP", email, code); }
    @Override public void sendPasswordReset(String email, String code) { jobs.enqueueEmail("RESET", email, code); }
    @Override public void sendOrderPlacedConfirmation(String email, String id) { jobs.enqueueEmail("PLACED", email, id); }
    @Override public void sendOrderStatusUpdate(String email, String id, String status) { jobs.enqueueEmail("STATUS", email, id, status); }
    @Override public void sendPickupScheduleNotification(String email, String id, String date, String slot, String location, String notes) {
        jobs.enqueueEmail("PICKUP", email, id, date, slot, location, notes);
    }
    @Override public void sendOrderCancelledNotification(String email, String id, String reason, String actor) {
        jobs.enqueueEmail("CANCELLED", email, id, reason, actor);
    }
}
