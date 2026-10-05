package com.uitmerch.backend.performance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uitmerch.backend.auth.dto.LoginRequest;
import com.uitmerch.backend.auth.entity.User;
import com.uitmerch.backend.auth.entity.OtpToken;
import com.uitmerch.backend.auth.repository.*;
import com.uitmerch.backend.auth.service.AuthService;
import com.uitmerch.backend.common.model.*;
import com.uitmerch.backend.common.service.*;
import com.uitmerch.backend.ai.service.*;
import com.uitmerch.backend.organization.entity.Organization;
import com.uitmerch.backend.organization.repository.OrganizationRepository;
import com.uitmerch.backend.merch.entity.MerchItem;
import com.uitmerch.backend.merch.repository.MerchItemRepository;
import com.uitmerch.backend.order.entity.*;
import com.uitmerch.backend.order.repository.*;
import com.uitmerch.backend.pickup.*;
import com.uitmerch.backend.event.entity.Event;
import com.uitmerch.backend.event.repository.EventRepository;
import com.uitmerch.backend.campaign.*;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.slf4j.LoggerFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.*;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

/** Opt-in local HTTP benchmark, never uses the project .env (run via scripts/performance-audit.sh). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "app.jwt.secret=uitmerch-disposable-performance-test-secret-2026",
    "app.delivery.enabled=false", "app.campaigns.enabled=false",
    "app.proxy.trusted-ips=127.0.0.1", "logging.level.root=WARN",
    "logging.level.com.uitmerch=WARN", "logging.level.org.springframework=WARN",
    "logging.level.org.hibernate.stat=OFF", "logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener=OFF",
    "spring.jpa.properties.hibernate.generate_statistics=true",
    "spring.datasource.hikari.maximum-pool-size=5", "spring.datasource.hikari.minimum-idle=1"
})
@ActiveProfiles("docker")
@EnabledIfEnvironmentVariable(named = "UITMERCH_PERFORMANCE_AUDIT", matches = "true")
class ApiPerformanceAuditTest {
    @DynamicPropertySource static void database(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", () -> System.getenv("UITMERCH_TEST_DATABASE_URL"));
        r.add("spring.datasource.username", () -> "postgres");
        r.add("spring.datasource.password", () -> "uitmerch_test_only");
    }
    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @Autowired @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping mappings;
    @Autowired EntityManagerFactory emf;
    @Autowired PasswordEncoder passwords;
    @Autowired AuthService auth;
    @Autowired UserRepository users;
    @Autowired OtpTokenRepository otps;
    @Autowired OrganizationRepository organizations;
    @Autowired MerchItemRepository merch;
    @Autowired OrderRepository orders;
    @Autowired OrderItemRepository lines;
    @Autowired EventRepository events;
    @Autowired CampaignService campaigns;
    @Autowired PickupService pickup;
    @Autowired GuestPickupReceiptRepository receipts;
    @Autowired com.zaxxer.hikari.HikariDataSource dataSource;
    @MockBean(name = "mailTransport") EmailService mail;
    @MockBean StorageService storage;
    @MockBean MerchEmbeddingService embeddings;
    @MockBean VisionAiService vision;
    @MockBean EmbeddingService embedding;

    final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
        .version(HttpClient.Version.HTTP_1_1).build();
    final AtomicInteger sequence = new AtomicInteger();
    User customer, organizer, admin;
    Organization org;
    MerchItem item, campaignItem;
    UUID orderId, guestId, eventId, campaignId, scheduleId;
    String customerToken, organizerToken, adminToken, hash;
    List<Map<String,Object>> results = new ArrayList<>();
    record Route(String method, String path, String handler) {}
    record Request(String path, String token, Object body, byte[] multipart) {}
    record Sample(double ms, int status, long bytes, long statements, String error) {}

    User user(UserRole role, boolean verified) {
        return users.save(User.builder().email("perf-" + UUID.randomUUID() + "@uit.edu.vn")
            .fullName("Performance User").passwordHash(hash).role(role).isVerified(verified).build());
    }
    String token(User u) {
        LoginRequest r = new LoginRequest(); r.setEmail(u.getEmail()); r.setPassword("Password1");
        return auth.login(r).getToken();
    }
    Organization organization() {
        return organizations.save(Organization.builder().ownerId(organizer.getId())
            .name("Performance " + UUID.randomUUID()).status(OrganizationStatus.ACTIVE).build());
    }
    MerchItem product() {
        return merch.save(MerchItem.builder().orgId(org.getId()).name("Performance Product " + UUID.randomUUID())
            .price(new BigDecimal("100000")).stock(100000).status(MerchItemStatus.PUBLISHED).build());
    }
    UUID order(boolean guest, OrderStatus status) {
        Order o = orders.save(Order.builder().orgId(org.getId()).userId(guest ? null : customer.getId())
            .guestName(guest ? "Guest" : null).guestEmail(guest ? "guest@uit.edu.vn" : null)
            .guestPhone(guest ? "0901234567" : null).status(status).totalAmount(new BigDecimal("100000")).build());
        lines.save(OrderItem.builder().orderId(o.getId()).merchId(item.getId()).merchName(item.getName())
            .unitPrice(item.getPrice()).quantity(1).subtotal(item.getPrice()).build());
        return o.getId();
    }
    UUID event() {
        return events.save(Event.builder().orgId(org.getId()).title("Performance Event")
            .status(EventStatus.PUBLISHED).build()).getId();
    }
    UUID campaign() {
        var p = product();
        return campaigns.create(organizer.getId(), org.getId(), new CampaignRequests.Create("Performance Campaign",
            null, 10000, Instant.now().plusSeconds(86400), List.of(new CampaignRequests.Variant(p.getId(), "M")))).id();
    }
    UUID cart(int size) {
        jdbc.update("DELETE FROM cart_items WHERE cart_id IN (SELECT id FROM carts WHERE user_id=?)", customer.getId());
        jdbc.update("DELETE FROM carts WHERE user_id=?", customer.getId());
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO carts(id,user_id,status) VALUES (?,?,'ACTIVE')", id, customer.getId());
        jdbc.update("INSERT INTO cart_items(id,cart_id,merch_id,quantity) SELECT gen_random_uuid(),?,id,1 FROM merch_items WHERE org_id=? AND id<>? AND status='PUBLISHED' ORDER BY id LIMIT ?",
            id, org.getId(), campaignItem.getId(), size);
        return jdbc.queryForObject("SELECT id FROM cart_items WHERE cart_id=? ORDER BY id LIMIT 1", UUID.class, id);
    }
    void seed() {
        hash = passwords.encode("Password1");
        customer = user(UserRole.CUSTOMER, true); organizer = user(UserRole.ORGANIZER, true); admin = user(UserRole.ADMIN, true);
        org = organization(); item = product(); campaignItem = product();
        customerToken = token(customer); organizerToken = token(organizer); adminToken = token(admin);
        orderId = order(false, OrderStatus.READY); guestId = order(true, OrderStatus.READY); eventId = event();
        campaignId = campaigns.create(organizer.getId(), org.getId(), new CampaignRequests.Create("Active Campaign", null,
            10000, Instant.now().plusSeconds(86400), List.of(new CampaignRequests.Variant(campaignItem.getId(), "M")))).id();
        scheduleId = UUID.randomUUID();
        jdbc.update("INSERT INTO pickup_schedules(id,org_id,pickup_date,pickup_time_slot,location) VALUES (?,?,CURRENT_DATE,'09:00-10:00','UIT')", scheduleId, org.getId());
        jdbc.update("UPDATE orders SET pickup_schedule_id=? WHERE id=?", scheduleId, orderId);
        jdbc.update("INSERT INTO merch_items(id,org_id,name,price,stock,status) SELECT gen_random_uuid(),?,'Performance Product '||g,100000,100000,'PUBLISHED' FROM generate_series(1,1000) g", org.getId());
        jdbc.update("INSERT INTO merch_images(id,merch_id,url,position) SELECT gen_random_uuid(),id,'https://example.com/test.png',0 FROM merch_items WHERE org_id=?", org.getId());
        jdbc.update("INSERT INTO orders(id,user_id,org_id,total_amount,status,pickup_schedule_id) SELECT gen_random_uuid(),?,?,100000,CASE WHEN g%2=0 THEN 'COMPLETED'::order_status ELSE 'READY'::order_status END,? FROM generate_series(1,1000) g", customer.getId(), org.getId(), scheduleId);
        jdbc.update("INSERT INTO order_items(id,order_id,merch_id,merch_name,unit_price,quantity,subtotal) SELECT gen_random_uuid(),o.id,?,'Performance Product',100000,1,100000 FROM orders o WHERE o.org_id=?", item.getId(), org.getId());
        jdbc.update("INSERT INTO events(id,org_id,title,status) SELECT gen_random_uuid(),?,'Performance Event '||g,'PUBLISHED' FROM generate_series(1,100) g", org.getId());
        jdbc.update("INSERT INTO event_merch(event_id,merch_id) SELECT id,? FROM events WHERE org_id=?", item.getId(), org.getId());
        for (var u : List.of(customer, organizer)) {
            jdbc.update("INSERT INTO notifications(id,user_id,title,message,type,is_read) SELECT gen_random_uuid(),?,'Test','Test notification','ORDER_CONFIRMED',false FROM generate_series(1,1000)", u.getId());
        }
        campaigns.reserve(customer.getId(), campaignId, new CampaignRequests.Reserve(campaignItem.getId(), 1, UUID.randomUUID(), null));
        cart(20);
        jdbc.execute("ANALYZE");
        when(vision.describeImage(any(), anyString())).thenReturn("Performance, blue");
        when(embedding.embed(anyString())).thenReturn(new float[768]);
        when(embeddings.findNearest(any(), anyInt())).thenReturn(List.of());
    }

    Request prepare(Route r) throws Exception {
        String p = r.path(), t = p.contains("/admin/") ? adminToken : p.contains("/organizations") && !p.contains("/public/") || p.contains("/organizer/") ? organizerToken : p.contains("/customer/") ? customerToken : null;
        Object body = null;
        UUID id = item.getId(), oid = orderId;
        if (p.contains("/auth/")) {
            if (p.endsWith("/register") || p.endsWith("/register/organizer")) body = Map.of("email", "new-"+UUID.randomUUID()+"@uit.edu.vn", "password","Password1","fullName","Performance New User");
            else {
                User u = user(UserRole.CUSTOMER, !p.endsWith("verify-email") && !p.endsWith("resend-otp"));
                if (p.endsWith("/login")) body = Map.of("email",u.getEmail(),"password","Password1");
                else if (p.endsWith("/refresh")) {
                    LoginRequest lr = new LoginRequest(); lr.setEmail(u.getEmail()); lr.setPassword("Password1");
                    body = Map.of("refreshToken",auth.login(lr).getRefreshToken());
                } else if (p.endsWith("/logout")) t=token(u);
                else if (p.endsWith("verify-email") || p.endsWith("reset-password")) {
                    otps.save(OtpToken.builder().user(u).otpCode("123456").passwordReset(p.endsWith("reset-password"))
                        .expiresAt(LocalDateTime.now().plusMinutes(15)).build());
                    body=Map.of("email",u.getEmail(),"otpCode","123456","newPassword","NewPassword1");
                } else body=Map.of("email",u.getEmail());
            }
        }
        if (p.endsWith("/dev/otps")) {
            User u=user(UserRole.CUSTOMER,false); otps.save(OtpToken.builder().user(u).otpCode("123456").expiresAt(LocalDateTime.now().plusMinutes(15)).build());
            p += "?email="+u.getEmail();
        }
        if (p.contains("/users/{id}")) {
            id=user(UserRole.CUSTOMER,true).getId(); body=p.endsWith("/role") ? Map.of("role","ORGANIZER") : null;
            if (p.endsWith("/active")) p+="?active=false";
        } else if (p.equals("/api/v1/admin/organizations/{id}/status")) { id=organization().getId(); body=Map.of("status","INACTIVE"); }
        if (p.equals("/api/v1/organizations") && r.method().equals("POST")) body=Map.of("name","Performance New Organization");
        if (p.equals("/api/v1/organizations/{id}")) { id=org.getId(); body=Map.of("name","Performance Updated Organization"); }
        if (p.contains("/public/organizations/{id}")) id=org.getId();
        if (p.contains("/merchs") && !r.method().equals("GET")) {
            if (!r.method().equals("POST")) id=product().getId();
            body=Map.of("name","Performance Merch","price",100000,"stock",100000,"imageUrls",List.of("https://example.com/test.png"));
        }
        if (p.contains("/events/{id}")) {
            id=r.method().equals("GET") ? eventId : event();
            if (p.endsWith("/merch/{merchId}")) jdbc.update("INSERT INTO event_merch(event_id,merch_id) VALUES (?,?)",id,item.getId());
            body=p.endsWith("/merch") ? Map.of("merchId",item.getId()) : Map.of("title","Performance Updated Event");
        }
        if (p.endsWith("/events") && r.method().equals("POST")) body=Map.of("title","Performance Event","status","PUBLISHED");
        if (p.contains("/public/events/{id}")) id=eventId;
        if (p.contains("/campaigns/{id}")) id=campaignId;
        if (p.endsWith("/campaigns") && r.method().equals("POST")) {
            var m=product(); body=Map.of("title","New Campaign","minimumQuantity",10000,"deadline",Instant.now().plusSeconds(86400).toString(),"variants",List.of(Map.of("merchId",m.getId(),"label","M")));
        }
        if (p.endsWith("/campaigns/{id}/cancel")) id=campaign();
        if (p.endsWith("/reservations")) body=Map.of("merchId",campaignItem.getId(),"quantity",1,"requestId",UUID.randomUUID());
        if (p.contains("/orders/{id}")) id=orderId;
        if (p.contains("/orders/") && !r.method().equals("GET")) {
            if (p.endsWith("/status")) { id=order(false,OrderStatus.PENDING); body=Map.of("status","CONFIRMED"); }
            if (p.endsWith("/cancel")) { id=order(false,OrderStatus.PENDING); body=Map.of("cancelReason","Changed mind"); }
            if (p.endsWith("/checkin")) id=order(false,OrderStatus.READY);
            if (p.endsWith("/instant")) body=Map.of("merchId",item.getId(),"quantity",1);
            if (p.endsWith("/pickup-token") && !p.contains("/public/")) oid=order(false,OrderStatus.READY);
            if (p.endsWith("/pickup/verify") || p.endsWith("/pickup/checkin")) {
                UUID o=order(false,OrderStatus.READY); String credential=pickup.issue(customer.getId(),o).token(); body=Map.of("token",credential);
            }
        }
        if (p.startsWith("/api/v1/public/orders")) {
            oid=guestId;
            if (r.method().equals("GET")) p+="?email=guest@uit.edu.vn";
            else if (p.endsWith("/pickup-receipt")) { oid=order(true,OrderStatus.READY); body=Map.of("email","guest@uit.edu.vn"); }
            else if (p.endsWith("/pickup-token")) {
                oid=order(true,OrderStatus.READY); byte[] raw=new byte[32]; new java.security.SecureRandom().nextBytes(raw);
                String credential=Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
                GuestPickupReceipt receipt=new GuestPickupReceipt(); receipt.setOrderId(oid); receipt.setTokenHash(HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(credential.getBytes(StandardCharsets.UTF_8)))); receipt.setExpiresAt(Instant.now().plusSeconds(900)); receipts.save(receipt);
                body=Map.of("receiptToken",credential);
            } else body=Map.of("guestName","Guest","guestPhone","0901234567","guestEmail","guest@uit.edu.vn","items",List.of(Map.of("merchId",item.getId(),"quantity",1)));
        }
        if (p.endsWith("/pickup-schedules") && r.method().equals("POST")) {
            body=Map.of("pickupDate",LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusDays(1).toString(),"pickupTimeSlot","09:00-10:00","location","UIT","orderIds",List.of(order(false,OrderStatus.CONFIRMED)));
        }
        if (p.endsWith("/profile") && r.method().equals("PATCH")) body=Map.of("fullName","Performance Updated User");
        if (p.contains("/cart") && !r.method().equals("GET")) {
            UUID cid=cart(20); p=p.replace("{itemId}",cid.toString());
            body=p.endsWith("/items") ? Map.of("merchId",product().getId(),"quantity",1) : Map.of("quantity",2);
            if (p.endsWith("/checkout")) body=Map.of();
        }
        if (p.contains("/wishlist/{merchId}") && r.method().equals("DELETE")) {
            UUID w=jdbc.queryForObject("SELECT id FROM wishlists WHERE user_id=?",UUID.class,customer.getId());
            jdbc.update("INSERT INTO wishlist_items(id,wishlist_id,merch_id) VALUES (?,?,?) ON CONFLICT DO NOTHING",UUID.randomUUID(),w,item.getId());
        }
        if (p.contains("/wishlist/{merchId}") && r.method().equals("POST")) {
            jdbc.update("DELETE FROM wishlist_items WHERE wishlist_id IN (SELECT id FROM wishlists WHERE user_id=?) AND merch_id=?",customer.getId(),item.getId());
        }
        if (p.contains("/following/{orgId}")) {
            if (!r.method().equals("POST")) jdbc.update("INSERT INTO organization_follows(id,user_id,org_id,notify_merch,notify_events,email_enabled,followed_at) VALUES (?,?,?,true,true,false,NOW()) ON CONFLICT DO NOTHING",UUID.randomUUID(),customer.getId(),org.getId());
            if (!r.method().equals("POST")) jdbc.update("UPDATE organization_follows SET enabled=true WHERE user_id=? AND org_id=?",customer.getId(),org.getId());
            body=Map.of("notifyMerch",true,"notifyEvents",true,"emailEnabled",false);
        }
        if (p.endsWith("/restock-subscriptions") && r.method().equals("POST")) body=Map.of("merchId",item.getId(),"emailEnabled",false);
        if (p.contains("/notifications/{id}/read")) {
            UUID u=p.contains("/organizer/") ? organizer.getId() : customer.getId();
            id=UUID.randomUUID(); jdbc.update("INSERT INTO notifications(id,user_id,title,message,type) VALUES (?,?,'Test','Test','ORDER_CONFIRMED')",id,u);
        }
        p=p.replace("{id}",id.toString()).replace("{orgId}",org.getId().toString()).replace("{merchId}",item.getId().toString())
            .replace("{orderId}",oid.toString()).replace("{campaignId}",campaignId.toString()).replace("{scheduleId}",scheduleId.toString());
        byte[] multipart=null;
        if (p.endsWith("/visual-search")) {
            var out=new java.io.ByteArrayOutputStream(); javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(1,1,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",out);
            var payload=new java.io.ByteArrayOutputStream(); payload.write("--perf-boundary\r\nContent-Disposition: form-data; name=\"image\"; filename=\"test.png\"\r\nContent-Type: image/png\r\n\r\n".getBytes(StandardCharsets.UTF_8)); payload.write(out.toByteArray()); payload.write("\r\n--perf-boundary--\r\n".getBytes(StandardCharsets.UTF_8)); multipart=payload.toByteArray();
        }
        if (r.method().equals("GET") && !p.contains("?") && !p.endsWith("/stream")) p+="?size=20";
        return new Request(p,t,body,multipart);
    }

    Sample send(Route route, Request req, boolean countSql) throws Exception {
        var b=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+req.path())).timeout(Duration.ofSeconds(30))
            .header("X-Forwarded-For","198.18."+(sequence.incrementAndGet()/250%250)+"."+(sequence.get()%250+1));
        if(req.token()!=null) b.header("Authorization","Bearer "+req.token());
        HttpRequest.BodyPublisher body=HttpRequest.BodyPublishers.noBody();
        if(req.multipart()!=null) { b.header("Content-Type","multipart/form-data; boundary=perf-boundary"); body=HttpRequest.BodyPublishers.ofByteArray(req.multipart()); }
        else if(req.body()!=null) { b.header("Content-Type","application/json"); body=HttpRequest.BodyPublishers.ofString(json.writeValueAsString(req.body())); }
        HttpRequest request=b.method(route.method(),body).build();
        var stats=emf.unwrap(SessionFactory.class).getStatistics(); long sql=countSql ? stats.getPrepareStatementCount() : 0;
        long start=System.nanoTime();
        if(route.path().endsWith("/stream")) {
            var response=client.send(request,HttpResponse.BodyHandlers.ofInputStream());
            try(var stream=response.body()) {
                String line=new java.io.BufferedReader(new java.io.InputStreamReader(stream)).readLine();
                return new Sample((System.nanoTime()-start)/1e6,response.statusCode(),line==null ? 0 : line.length(),stats.getPrepareStatementCount()-sql,"");
            }
        }
        var response=client.send(request,HttpResponse.BodyHandlers.ofByteArray());
        return new Sample((System.nanoTime()-start)/1e6,response.statusCode(),response.body().length,countSql ? stats.getPrepareStatementCount()-sql : 0,
            response.statusCode()>=400 ? new String(response.body(),StandardCharsets.UTF_8).substring(0,Math.min(300,response.body().length)) : "");
    }
    static double percentile(List<Double> values,double quantile) {
        var sorted=values.stream().sorted().toList(); return sorted.get(Math.max(0,(int)Math.ceil(sorted.size()*quantile)-1));
    }
    Map<String,Object> summarize(Route r,List<Sample> samples) {
        Map<String,Object> result=new LinkedHashMap<>(); result.put("method",r.method()); result.put("path",r.path()); result.put("handler",r.handler()); result.put("samples",samples.size());
        var times=samples.stream().map(Sample::ms).toList();
        result.put("p50_ms",percentile(times,.5)); result.put("p95_ms",percentile(times,.95)); result.put("p99_ms",percentile(times,.99));
        result.put("mean_ms",times.stream().mapToDouble(Double::doubleValue).average().orElse(0)); result.put("max_ms",Collections.max(times));
        result.put("latencies_ms",times);
        Map<Integer,Long> statuses=new TreeMap<>(); samples.forEach(s->statuses.merge(s.status(),1L,Long::sum)); result.put("statuses",statuses);
        result.put("hibernate_statements_mean",samples.stream().mapToLong(Sample::statements).average().orElse(0)); result.put("bytes_mean",samples.stream().mapToLong(Sample::bytes).average().orElse(0));
        result.put("errors",samples.stream().map(Sample::error).filter(s->!s.isEmpty()).distinct().toList());
        result.put("mode",r.path().endsWith("/stream") ? "SSE first line" : r.path().endsWith("visual-search") ? "AI stub + real keyword fallback" : "HTTP + PostgreSQL");
        return result;
    }
    List<Route> inventory() {
        List<Route> routes=new ArrayList<>();
        mappings.getHandlerMethods().forEach((info,handler)->{
            if(!handler.getBeanType().getPackageName().startsWith("com.uitmerch")) return;
            for(String path:info.getPatternValues()) for(var method:info.getMethodsCondition().getMethods())
                routes.add(new Route(method.name(),path,handler.getBeanType().getSimpleName()+"."+handler.getMethod().getName()));
        });
        return routes.stream().sorted(Comparator.comparing(Route::path).thenComparing(Route::method)).toList();
    }
    void save(Map<String,Object> report) throws Exception {
        Path output=Path.of("target/performance-audit.json"); Files.createDirectories(output.getParent());
        json.writerWithDefaultPrettyPrinter().writeValue(output.toFile(),report);
    }
    @Test void auditAllRoutes() throws Exception {
        assertThat(System.getenv("UITMERCH_TEST_DATABASE_URL")).startsWith("jdbc:postgresql://127.0.0.1:");
        // Some logback logger-specific DEBUG settings survive profile overrides;
        // turn them off explicitly so SQL console logging does not distort timings.
        var logging=(ch.qos.logback.classic.LoggerContext) LoggerFactory.getILoggerFactory();
        logging.getLoggerList().forEach(logger->logger.setLevel(ch.qos.logback.classic.Level.WARN));
        seed(); List<Route> routes=inventory(); Map<String,Object> report=new LinkedHashMap<>();
        report.put("started_at",Instant.now().toString()); report.put("java",System.getProperty("java.version"));
        report.put("database",jdbc.queryForObject("SELECT version()",String.class)); report.put("pool_size",5);
        report.put("warmups",5); report.put("sample_count",30); report.put("routes",routes); report.put("baseline",results);
        Map<String,Long> counts=new LinkedHashMap<>(); for(String table:List.of("users","organizations","merch_items","merch_images","orders","order_items","events","notifications")) counts.put(table,jdbc.queryForObject("SELECT COUNT(*) FROM "+table,Long.class)); report.put("seed_counts",counts);
        for(Route route:routes) {
            List<Sample> samples=new ArrayList<>();
            // Keep account/global-limited endpoints under their real 20/minute
            // limit. Do not benchmark rejection responses as successful work.
            int count=route.path().equals("/api/v1/customer/orders/{orderId}/pickup-token") || route.path().endsWith("/visual-search") ? 15 : 30;
            for(int i=0;i<count+5;i++) { Request req=prepare(route); Sample sample=send(route,req,true); if(i>=5) samples.add(sample); }
            var summary=summarize(route,samples); results.add(summary); save(report);
            System.out.println("PERF "+route.method()+" "+route.path()+" p95="+summary.get("p95_ms")+" statuses="+summary.get("statuses"));
        }
        List<Map<String,Object>> loads=new ArrayList<>(); report.put("load",loads);
        for(String path:List.of("/api/v1/public/merch","/api/v1/public/merch/popular","/api/v1/customer/orders","/api/v1/customer/cart","/api/v1/organizations/{orgId}/analytics","/api/v1/public/orders/{orderId}")) {
            Route route=routes.stream().filter(r->r.path().equals(path)&&r.method().equals("GET")).findFirst().orElseThrow();
            Request req=prepare(route);
            for(int concurrency:List.of(1,5,10,20)) {
                int n=200; List<Sample> samples=new ArrayList<>();
                AtomicInteger pendingMax=new AtomicInteger(); AtomicInteger activeMax=new AtomicInteger();
                var monitor=Executors.newSingleThreadScheduledExecutor();
                monitor.scheduleAtFixedRate(()->{
                    var pool=dataSource.getHikariPoolMXBean();
                    pendingMax.accumulateAndGet(pool.getThreadsAwaitingConnection(),Math::max);
                    activeMax.accumulateAndGet(pool.getActiveConnections(),Math::max);
                },0,10,TimeUnit.MILLISECONDS);
                long start=System.nanoTime();
                try(var executor=Executors.newFixedThreadPool(concurrency)) {
                    List<Future<Sample>> futures=new ArrayList<>(); for(int i=0;i<n;i++) futures.add(executor.submit(()->send(route,req,false)));
                    for(var future:futures) samples.add(future.get(60,TimeUnit.SECONDS));
                }
                monitor.shutdownNow();
                double seconds=(System.nanoTime()-start)/1e9; var summary=summarize(route,samples); summary.put("concurrency",concurrency); summary.put("rps",n/seconds);
                summary.put("pool_pending_max",pendingMax.get()); summary.put("pool_active_max",activeMax.get());
                loads.add(summary); save(report);
                System.out.println("LOAD "+path+" c="+concurrency+" p95="+summary.get("p95_ms")+" rps="+summary.get("rps")+" statuses="+summary.get("statuses"));
            }
        }
        List<Map<String,Object>> sizes=new ArrayList<>(); report.put("page_sizes",sizes);
        for(String path:List.of("/api/v1/customer/orders","/api/v1/admin/orders","/api/v1/public/merch")) {
            Route route=routes.stream().filter(r->r.path().equals(path)&&r.method().equals("GET")).findFirst().orElseThrow();
            for(int size:List.of(1,20,100)) {
                Request base=prepare(route); Request req=new Request(base.path().replace("size=20","size="+size),base.token(),null,null);
                List<Sample> samples=new ArrayList<>(); for(int i=0;i<35;i++) { Sample sample=send(route,req,true); if(i>=5)samples.add(sample); }
                var summary=summarize(route,samples); summary.put("page_size",size); sizes.add(summary); save(report);
            }
        }
        // After optimization migrations are installed, adding a second index
        // is not an unindexed/indexed comparison. Keep DDL experiments opt-in.
        report.put("sql_experiments", "true".equals(System.getenv("UITMERCH_PERFORMANCE_SQL_EXPERIMENTS"))
            ? sqlExperiments() : List.of());
        report.put("finished_at",Instant.now().toString()); save(report);
        assertThat(results).hasSize(routes.size());
        assertThat(results.stream().filter(r->!((List<?>)r.get("errors")).isEmpty()).toList()).as("Every baseline route must have a successful fixture").isEmpty();
    }

    List<Map<String,Object>> sqlExperiments() {
        List<Map<String,Object>> experiments=new ArrayList<>();
        // Query-level experiments use only disposable data. No application code
        // or migration is changed, and these are not HTTP speedup measurements.
        User other=user(UserRole.CUSTOMER,true);
        jdbc.update("INSERT INTO orders(id,user_id,org_id,total_amount,status) SELECT gen_random_uuid(),?,?,100000,'PENDING' FROM generate_series(1,100000)",other.getId(),org.getId());
        jdbc.execute("ANALYZE orders");
        String query="SELECT * FROM orders WHERE user_id=? ORDER BY created_at DESC LIMIT 20";
        Map<String,Object> index=new LinkedHashMap<>(); index.put("name","customer_order_sort_index"); index.put("rows",jdbc.queryForObject("SELECT COUNT(*) FROM orders",Long.class));
        index.put("before_plan",jdbc.queryForList("EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON) "+query,customer.getId()));
        index.put("before_ms",timeSql(()->jdbc.queryForList(query,customer.getId())));
        jdbc.execute("CREATE INDEX perf_orders_user_created ON orders(user_id,created_at DESC,id DESC)");
        index.put("after_plan",jdbc.queryForList("EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON) "+query,customer.getId()));
        index.put("after_ms",timeSql(()->jdbc.queryForList(query,customer.getId()))); experiments.add(index);
        List<UUID> ids=jdbc.queryForList("SELECT id FROM orders WHERE user_id=? ORDER BY created_at DESC LIMIT 20",UUID.class,customer.getId());
        Map<String,Object> batch=new LinkedHashMap<>(); batch.put("name","order_items_20_queries_vs_batch");
        batch.put("before_ms",timeSql(()->ids.forEach(id->jdbc.queryForList("SELECT * FROM order_items WHERE order_id=?",id))));
        String placeholders=String.join(",",Collections.nCopies(ids.size(),"?"));
        batch.put("after_ms",timeSql(()->jdbc.queryForList("SELECT * FROM order_items WHERE order_id IN ("+placeholders+")",ids.toArray()))); experiments.add(batch);
        String search="SELECT id FROM merch_items WHERE lower(name) LIKE '%product 999%' AND status='PUBLISHED'";
        Map<String,Object> trigram=new LinkedHashMap<>(); trigram.put("name","substring_trigram_index");
        jdbc.update("INSERT INTO merch_items(id,org_id,name,price,stock,status) SELECT gen_random_uuid(),?,'Performance Product '||g,100000,100000,'PUBLISHED' FROM generate_series(1001,100000) g",org.getId());
        jdbc.execute("ANALYZE merch_items");
        trigram.put("rows",jdbc.queryForObject("SELECT COUNT(*) FROM merch_items",Long.class));
        trigram.put("before_plan",jdbc.queryForList("EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON) "+search)); trigram.put("before_ms",timeSql(()->jdbc.queryForList(search)));
        jdbc.execute("CREATE EXTENSION IF NOT EXISTS pg_trgm");
        jdbc.execute("CREATE INDEX perf_merch_name_trgm ON merch_items USING gin(lower(name) gin_trgm_ops) WHERE status='PUBLISHED'");
        trigram.put("after_plan",jdbc.queryForList("EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON) "+search)); trigram.put("after_ms",timeSql(()->jdbc.queryForList(search))); experiments.add(trigram);
        return experiments;
    }
    Map<String,Double> timeSql(Runnable operation) {
        List<Double> times=new ArrayList<>();
        for(int i=0;i<35;i++) { long start=System.nanoTime(); operation.run(); if(i>=5)times.add((System.nanoTime()-start)/1e6); }
        return Map.of("p50",percentile(times,.5),"p95",percentile(times,.95),"p99",percentile(times,.99));
    }
}
