package com.uitmerch.backend.order.security;
import com.uitmerch.backend.common.model.OrderStatus;
import com.uitmerch.backend.order.repository.OrderRepository;
import com.uitmerch.backend.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import java.time.Instant;
@Component @RequiredArgsConstructor @Slf4j
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="app.checkout.expiry-enabled",havingValue="true",matchIfMissing=true)
public class PendingOrderExpiryWorker {
    private final OrderRepository orders;
    private final OrderService service;
    @Scheduled(fixedDelay=60000,initialDelay=60000)
    public void expire() {
        for (var order:orders.findTop100ByStatusAndPendingExpiresAtBeforeOrderByPendingExpiresAtAsc(OrderStatus.PENDING,Instant.now())) {
            try { service.expirePendingOrder(order.getId()); }
            catch (Exception ex) { log.warn("Unable to expire pending order {}; will retry",order.getId()); }
        }
    }
}
