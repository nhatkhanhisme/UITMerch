package com.uitmerch.backend.notification.announcement;

import com.uitmerch.backend.common.model.NotificationType;
import org.springframework.data.domain.Pageable;
import java.util.*;

public interface AnnouncementAudience {
    interface Recipient {
        UUID getUserId();
        String getEmail();
        boolean getEmailEnabled();
    }
    boolean supports(NotificationType kind);
    List<Recipient> recipients(AnnouncementEvent event, Pageable limit);
}
