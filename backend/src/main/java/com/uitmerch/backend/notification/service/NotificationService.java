package com.uitmerch.backend.notification.service;

import com.uitmerch.backend.common.exception.ResourceNotFoundException;
import com.uitmerch.backend.common.model.NotificationType;
import com.uitmerch.backend.notification.dto.NotificationResponse;
import com.uitmerch.backend.notification.entity.Notification;
import com.uitmerch.backend.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SseEmitterManager sseEmitterManager;

    @Transactional
    public void push(UUID userId, String title, String message, NotificationType type, UUID relatedOrderId) {
        Notification saved = notificationRepository.save(Notification.builder()
            .userId(userId)
            .title(title)
            .message(message)
            .type(type)
            .relatedOrderId(relatedOrderId)
            .build());
        sseEmitterManager.send(userId, NotificationResponse.from(saved));
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getForUser(UUID userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
            .map(NotificationResponse::from);
    }

    @Transactional(readOnly = true)
    public long countUnread(UUID userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public NotificationResponse markRead(UUID userId, UUID notificationId) {
        Notification n = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new ResourceNotFoundException("Notification", notificationId.toString()));
        if (!n.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Notification", notificationId.toString());
        }
        n.setRead(true);
        return NotificationResponse.from(notificationRepository.save(n));
    }

    @Transactional
    public int markAllRead(UUID userId) {
        return notificationRepository.markAllReadByUserId(userId);
    }
}
