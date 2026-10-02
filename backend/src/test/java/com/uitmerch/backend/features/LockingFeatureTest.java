package com.uitmerch.backend.features;
import com.uitmerch.backend.common.model.*;
import com.uitmerch.backend.common.exception.ValidationException;
import com.uitmerch.backend.order.dto.*;
import com.uitmerch.backend.order.service.OrderService;
import com.uitmerch.backend.order.repository.OrderRepository;
import com.uitmerch.backend.pickup.PickupService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.assertj.core.api.Assertions.*;
@EnabledIfEnvironmentVariable(named="UITMERCH_TEST_DATABASE_URL",matches="jdbc:postgresql:.*")
class LockingFeatureTest extends BackendFeatureTest {
    @Autowired OrderService orders;
    @Autowired OrderRepository orderRepository;
    @Autowired PickupService pickup;
    @Autowired JdbcTemplate jdbc;
    @Test void scanWaitingForOrderLockRejectsTokenReplacedBeforeItAcquiresLock() throws Exception {
        var org=organization(); var item=product(org,3); var customer=user(UserRole.CUSTOMER);
        InstantOrderRequest request=new InstantOrderRequest();request.setMerchId(item.getId());request.setQuantity(1);
        var order=orders.createInstantOrder(customer.getId(),request);
        orders.updateOrderStatus(org.getOwnerId(),org.getId(),order.getId(),OrderStatus.CONFIRMED);
        orders.updateOrderStatus(org.getOwnerId(),org.getId(),order.getId(),OrderStatus.READY);
        var old=pickup.issue(customer.getId(),order.getId());
        var fresh=new AtomicReference<PickupService.IssuedToken>();
        try(var executor=Executors.newSingleThreadExecutor()) {
            var scan=new AtomicReference<Future<Boolean>>();
            tx.executeWithoutResult(s->{
                orderRepository.findLockedById(order.getId()).orElseThrow();
                scan.set(executor.submit(()->{try{pickup.checkIn(org.getOwnerId(),org.getId(),old.token(),null);return true;}catch(ValidationException expected){return false;}}));
                Instant timeout=Instant.now().plusSeconds(10); boolean blocked=false;
                while(Instant.now().isBefore(timeout)) {
                    jdbc.execute("SELECT pg_stat_clear_snapshot()");
                    Long waiting=jdbc.queryForObject("SELECT count(*) FROM pg_stat_activity WHERE wait_event_type='Lock' AND query LIKE '%orders%' AND pid<>pg_backend_pid()",Long.class);
                    if(waiting>0){blocked=true;break;}
                    try{Thread.sleep(20);}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException(e);}
                }
                assertThat(blocked).as("scanner must be waiting for the locked order").isTrue();
                fresh.set(pickup.issue(customer.getId(),order.getId()));
            });
            assertThat(scan.get().get(10,TimeUnit.SECONDS)).isFalse();
        }
        assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.READY);
        assertThat(pickup.checkIn(org.getOwnerId(),org.getId(),fresh.get().token(),null).getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }
    @Test void multiSkuCancellationAndCheckoutUseSameLockOrderAcrossUuidSignBoundary() throws Exception {
        var org=organization(); var user=user(UserRole.CUSTOMER);
        // PostgreSQL UUID order is unsigned; Java UUID.compareTo places the 8... ID first.
        String suffix=UUID.randomUUID().toString().substring(8);
        var a=UUID.fromString("00000000"+suffix); var b=UUID.fromString("80000000"+suffix);
        for(UUID id:List.of(a,b)) jdbc.update("INSERT INTO merch_items(id,org_id,name,price,stock,status) VALUES(?,?,?,100000,3,'PUBLISHED')",id,org.getId(),"Lock boundary SKU");
        GuestOrderRequest request=new GuestOrderRequest();
        var lines=new ArrayList<GuestOrderItemRequest>();
        for(UUID id:List.of(a,b)){var line=new GuestOrderItemRequest();line.setMerchId(id);line.setQuantity(1);lines.add(line);}
        request.setItems(lines);
        var current=new AtomicReference<>(orders.createPublicOrder(user.getId(),request).getFirst());
        for(int round=0;round<10;round++) {
            var next=new AtomicReference<OrderResponse>(); var index=new AtomicInteger(); var old=current.get();
            assertThat(parallel(2,()->{if(index.getAndIncrement()==0)orders.cancelCustomerOrder(user.getId(),old.getId(),new CancelOrderRequest());else next.set(orders.createPublicOrder(user.getId(),request).getFirst());})).containsOnly(true);
            current.set(next.get());
            assertThat(merch.findById(a).orElseThrow().getStock()).isEqualTo(2);
            assertThat(merch.findById(b).orElseThrow().getStock()).isEqualTo(2);
        }
    }
}
