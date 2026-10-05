package com.uitmerch.backend.order.security;
import com.uitmerch.backend.common.delivery.BackgroundJobService;
import com.uitmerch.backend.common.exception.ResourceNotFoundException;
import com.uitmerch.backend.common.security.SecurityCredentials;
import com.uitmerch.backend.order.dto.OrderResponse;
import com.uitmerch.backend.order.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;
@Service @RequiredArgsConstructor
public class GuestTrackingService {
    private final GuestTrackingRepository credentials;
    private final OrderRepository orders;
    private final OrderItemRepository items;
    private final PickupScheduleRepository schedules;
    private final BackgroundJobService jobs;
    @Transactional
    public void request(UUID orderId,String email) {
        var candidate=orders.findLockedById(orderId);
        if (candidate.isEmpty()) return;
        var order=candidate.get();
        if (order.getUserId()!=null || order.getGuestEmail()==null || !order.getGuestEmail().equalsIgnoreCase(email.trim())) return;
        String token=SecurityCredentials.generate();
        var row=credentials.findById(orderId).orElseGet(GuestTrackingCredential::new);
        row.setOrderId(orderId);row.setTokenHash(SecurityCredentials.hash(token));row.setExpiresAt(Instant.now().plusSeconds(86400));
        credentials.save(row);
        jobs.enqueueEmail("ANNOUNCEMENT",order.getGuestEmail(),"Mã tra cứu đơn hàng",
            "Đơn "+orderId+". Mã tra cứu (hết hạn sau 24 giờ): "+token+". Nhập mã tại trang Tra cứu đơn khách. Không chia sẻ mã này.");
    }
    @Transactional(readOnly=true)
    public OrderResponse read(UUID id,String token) {
        var grant=credentials.findById(id).orElseThrow(()->missing(id));
        if (!grant.getExpiresAt().isAfter(Instant.now()) || !SecurityCredentials.matches(token,grant.getTokenHash())) throw missing(id);
        var order=orders.findById(id).filter(o->o.getUserId()==null).orElseThrow(()->missing(id));
        var schedule=order.getPickupScheduleId()==null?null:schedules.findById(order.getPickupScheduleId()).orElse(null);
        var result=OrderResponse.from(order,items.findByOrderId(id),schedule);
        result.setGuestName(null);result.setGuestEmail(null);result.setGuestPhone(null);result.setNote(null);result.setCancelReasonNote(null);
        if(result.getPickupSchedule()!=null) result.getPickupSchedule().setNotes(null);
        return result;
    }
    private ResourceNotFoundException missing(UUID id) { return new ResourceNotFoundException("Order",id.toString()); }
}
