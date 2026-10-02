package com.uitmerch.backend.notification.announcement;

import com.uitmerch.backend.common.delivery.BackgroundJobService;
import com.uitmerch.backend.common.model.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;

@Service @RequiredArgsConstructor
public class AnnouncementService {
    private final AnnouncementRepository events;
    private final BackgroundJobService jobs;

    @Transactional
    public void enqueue(String key, NotificationType kind, String title, String message,
                        UUID orgId, UUID merchId, UUID eventId) {
        // The caller holds the source row lock, serializing a resource's event generation.
        if (events.existsByDedupeKey(key)) return;
        AnnouncementEvent event = new AnnouncementEvent();
        event.setId(UUID.randomUUID()); event.setDedupeKey(key); event.setKind(kind);
        event.setTitle(title); event.setMessage(message); event.setOrgId(orgId);
        event.setMerchId(merchId); event.setEventId(eventId); event.setCreatedAt(Instant.now());
        events.save(event);
        jobs.enqueueAnnouncement(event.getId());
    }
}
