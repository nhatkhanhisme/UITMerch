package com.uitmerch.backend.notification.dto;

import com.uitmerch.backend.common.model.NotificationType;
import com.uitmerch.backend.notification.entity.Notification;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class NotificationResponse {

    private UUID id;
    private UUID userId;
    private String title;
    private String message;
    private NotificationType type;
    private boolean isRead;
    private UUID relatedOrderId;
    private LocalDateTime createdAt;

    public static NotificationResponse from(Notification n) {
        return NotificationResponse.builder()
            .id(n.getId())
            .userId(n.getUserId())
            .title(n.getTitle())
            .message(n.getMessage())
            .type(n.getType())
            .isRead(n.isRead())
            .relatedOrderId(n.getRelatedOrderId())
            .createdAt(n.getCreatedAt())
            .build();
    }
}
