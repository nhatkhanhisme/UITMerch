package com.uitmerch.backend.restock;

import com.uitmerch.backend.common.model.NotificationType;
import com.uitmerch.backend.notification.announcement.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import java.util.List;

@Component @RequiredArgsConstructor
public class RestockAudience implements AnnouncementAudience {
    private final RestockRepository subscriptions;
    @Override public boolean supports(NotificationType kind) { return kind == NotificationType.MERCH_RESTOCKED; }
    @Override public List<Recipient> recipients(AnnouncementEvent event, Pageable page) {
        return subscriptions.audience(event.getMerchId(), event.getCreatedAt(), event.getCursorUserId(), page);
    }
}
