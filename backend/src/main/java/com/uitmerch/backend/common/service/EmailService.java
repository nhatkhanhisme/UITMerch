package com.uitmerch.backend.common.service;

public interface EmailService {
    void sendAnnouncement(String toEmail, String title, String message);
    void sendOtp(String toEmail, String otpCode);
    void sendPasswordReset(String toEmail, String otpCode);
    void sendOrderPlacedConfirmation(String toEmail, String orderId);
    void sendOrderStatusUpdate(String toEmail, String orderId, String newStatus);
    void sendPickupScheduleNotification(String toEmail, String orderId,
                                        String pickupDate, String pickupTimeSlot,
                                        String location, String notes);
    void sendOrderCancelledNotification(String toEmail, String orderId,
                                        String cancelReason, String cancelledBy);
}
