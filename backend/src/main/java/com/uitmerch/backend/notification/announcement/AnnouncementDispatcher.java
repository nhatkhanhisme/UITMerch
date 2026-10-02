package com.uitmerch.backend.notification.announcement;

import com.uitmerch.backend.common.delivery.BackgroundJobService;
import com.uitmerch.backend.common.model.*;
import com.uitmerch.backend.common.service.EmailService;
import com.uitmerch.backend.merch.repository.MerchItemRepository;
import com.uitmerch.backend.organization.repository.OrganizationRepository;
import com.uitmerch.backend.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service @RequiredArgsConstructor
public class AnnouncementDispatcher {
    private final AnnouncementRepository events;
    private final List<AnnouncementAudience> audiences;
    private final OrganizationRepository organizations;
    private final MerchItemRepository merch;
    private final NotificationService notifications;
    private final EmailService mail;
    private final BackgroundJobService jobs;
    private final com.uitmerch.backend.event.repository.EventRepository scheduledEvents;

    @Transactional
    public void deliverBatch(UUID id) {
        var optional = events.findLockedById(id);
        if (optional.isEmpty() || optional.get().isFinished()) return;
        AnnouncementEvent event = optional.get();
        boolean eligible = organizations.findById(event.getOrgId())
            .filter(o -> o.getStatus() == OrganizationStatus.ACTIVE).isPresent();
        if (event.getMerchId() != null) {
            eligible &= merch.findById(event.getMerchId()).filter(m -> m.getStatus() == MerchItemStatus.PUBLISHED
                && m.getOrgId().equals(event.getOrgId())
                && (event.getKind() != NotificationType.MERCH_RESTOCKED || m.getStock() > 0)).isPresent();
        }
        if (event.getEventId() != null) {
            eligible &= scheduledEvents.findById(event.getEventId()).filter(e -> e.getStatus() == EventStatus.PUBLISHED
                && e.getOrgId().equals(event.getOrgId())).isPresent();
        }
        if (!eligible) { event.setFinished(true); return; }
        AnnouncementAudience audience = audiences.stream().filter(a -> a.supports(event.getKind())).findFirst()
            .orElseThrow(() -> new IllegalStateException("Unsupported announcement audience"));
        var recipients = audience.recipients(event, PageRequest.of(0, 50));
        for (var recipient : recipients) {
            if (notifications.pushAnnouncement(recipient.getUserId(), event) && recipient.getEmailEnabled()) {
                mail.sendAnnouncement(recipient.getEmail(), event.getTitle(), event.getMessage());
            }
            event.setCursorUserId(recipient.getUserId());
        }
        if (recipients.size() < 50) event.setFinished(true);
        else jobs.enqueueAnnouncement(id); // Persist a continuation in this same transaction.
    }
}
