package com.uitmerch.backend.restock;

import com.uitmerch.backend.auth.repository.UserRepository;
import com.uitmerch.backend.common.exception.*;
import com.uitmerch.backend.common.model.*;
import com.uitmerch.backend.merch.entity.MerchItem;
import com.uitmerch.backend.merch.repository.MerchItemRepository;
import com.uitmerch.backend.notification.announcement.AnnouncementService;
import com.uitmerch.backend.organization.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;

@Service @RequiredArgsConstructor
public class RestockService {
    private final RestockRepository subscriptions;
    private final UserRepository users;
    private final MerchItemRepository merch;
    private final OrganizationRepository organizations;
    private final AnnouncementService announcements;

    @Transactional
    public RestockSubscription subscribe(UUID userId, UUID merchId, boolean emailEnabled) {
        users.findLockedById(userId).filter(u -> u.isActive() && u.isVerified() && u.getRole() == UserRole.CUSTOMER)
            .orElseThrow(() -> new AuthenticationException("Invalid account"));
        if (merch.findPublicByIds(java.util.List.of(merchId)).isEmpty())
            throw new ResourceNotFoundException("Merch item", merchId.toString());
        var subscription = subscriptions.findByUserIdAndMerchId(userId, merchId).orElseGet(RestockSubscription::new);
        if (subscription.getId() == null || !subscription.isEnabled()) subscription.setSubscribedAt(Instant.now());
        subscription.setUserId(userId); subscription.setMerchId(merchId);
        subscription.setEnabled(true); subscription.setEmailEnabled(emailEnabled);
        return subscriptions.save(subscription);
    }
    @Transactional
    public void unsubscribe(UUID userId, UUID merchId) {
        users.findLockedById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));
        subscriptions.findByUserIdAndMerchId(userId, merchId).ifPresent(s -> s.setEnabled(false));
    }
    @Transactional(readOnly = true)
    public Page<RestockSubscription> list(UUID userId, Pageable page) {
        return subscriptions.findByUserIdAndEnabledTrue(userId, page);
    }
    // Called with the merchandise row locked, inside the stock writer's transaction.
    public void stockIncreased(MerchItem item, int previousStock) {
        if (previousStock != 0 || item.getStock() <= 0) return;
        item.setRestockCycle(item.getRestockCycle() + 1);
        if (item.getStatus() != MerchItemStatus.PUBLISHED || organizations.findById(item.getOrgId())
            .filter(o -> o.getStatus() == OrganizationStatus.ACTIVE).isEmpty()) return;
        announcements.enqueue("restock:" + item.getId() + ":" + item.getRestockCycle(), NotificationType.MERCH_RESTOCKED,
            "Sản phẩm đã có hàng trở lại", item.getName() + " đã có hàng trở lại.", item.getOrgId(), item.getId(), null);
    }
}
