package com.uitmerch.backend.following;
import com.uitmerch.backend.common.model.NotificationType;
import com.uitmerch.backend.notification.announcement.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import java.util.List;
@Component @RequiredArgsConstructor
public class FollowAudience implements AnnouncementAudience {
    private final FollowRepository follows;
    @Override public boolean supports(NotificationType kind) {
        return kind == NotificationType.MERCH_PUBLISHED || kind == NotificationType.EVENT_PUBLISHED;
    }
    @Override public List<Recipient> recipients(AnnouncementEvent event, Pageable page) {
        return follows.audience(event.getOrgId(), event.getCreatedAt(), event.getCursorUserId(),
            event.getKind() == NotificationType.MERCH_PUBLISHED, page);
    }
}
