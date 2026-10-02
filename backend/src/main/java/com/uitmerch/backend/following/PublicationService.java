package com.uitmerch.backend.following;
import com.uitmerch.backend.common.model.*;
import com.uitmerch.backend.merch.entity.MerchItem;
import com.uitmerch.backend.event.entity.Event;
import com.uitmerch.backend.notification.announcement.AnnouncementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
@Service @RequiredArgsConstructor
public class PublicationService {
    private final AnnouncementService announcements;
    // Source rows are new or locked by their existing publication service.
    public void merchPublished(MerchItem item) {
        if (item.getStatus() != MerchItemStatus.PUBLISHED || item.isPublicationAnnounced()) return;
        item.setPublicationAnnounced(true);
        announcements.enqueue("merch-published:" + item.getId(), NotificationType.MERCH_PUBLISHED,
            "Sản phẩm mới từ tổ chức bạn theo dõi", item.getName() + " vừa được xuất bản.", item.getOrgId(), item.getId(), null);
    }
    public void eventPublished(Event event) {
        if (event.getStatus() != EventStatus.PUBLISHED || event.isPublicationAnnounced()) return;
        event.setPublicationAnnounced(true);
        announcements.enqueue("event-published:" + event.getId(), NotificationType.EVENT_PUBLISHED,
            "Sự kiện mới từ tổ chức bạn theo dõi", event.getTitle() + " vừa được xuất bản.", event.getOrgId(), null, event.getId());
    }
}
