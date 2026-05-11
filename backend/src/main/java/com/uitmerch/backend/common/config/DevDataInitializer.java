package com.uitmerch.backend.common.config;

import com.uitmerch.backend.auth.entity.User;
import com.uitmerch.backend.auth.repository.UserRepository;
import com.uitmerch.backend.cart.entity.Cart;
import com.uitmerch.backend.cart.entity.CartItem;
import com.uitmerch.backend.cart.repository.CartItemRepository;
import com.uitmerch.backend.cart.repository.CartRepository;
import com.uitmerch.backend.common.model.*;
import com.uitmerch.backend.event.entity.Event;
import com.uitmerch.backend.event.entity.EventMerch;
import com.uitmerch.backend.event.repository.EventMerchRepository;
import com.uitmerch.backend.event.repository.EventRepository;
import com.uitmerch.backend.merch.entity.MerchItem;
import com.uitmerch.backend.merch.repository.MerchItemRepository;
import com.uitmerch.backend.order.entity.Order;
import com.uitmerch.backend.order.entity.OrderItem;
import com.uitmerch.backend.order.repository.OrderItemRepository;
import com.uitmerch.backend.order.repository.OrderRepository;
import com.uitmerch.backend.organization.entity.Organization;
import com.uitmerch.backend.organization.repository.OrganizationRepository;
import com.uitmerch.backend.wishlist.entity.Wishlist;
import com.uitmerch.backend.wishlist.entity.WishlistItem;
import com.uitmerch.backend.wishlist.repository.WishlistItemRepository;
import com.uitmerch.backend.wishlist.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
@Profile("dev | docker")
@RequiredArgsConstructor
@Slf4j
public class DevDataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final MerchItemRepository merchItemRepository;
    private final EventRepository eventRepository;
    private final EventMerchRepository eventMerchRepository;
    private final WishlistRepository wishlistRepository;
    private final WishlistItemRepository wishlistItemRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            log.info("Sample data already present, skipping seed.");
            return;
        }
        log.info("Seeding dev sample data...");

        // ── Users ─────────────────────────────────────────────────────────────
        User admin = saveUser("admin@uit.edu.vn",  "Admin123",  "Admin User",       null,          null,                         UserRole.ADMIN);
        User org1  = saveUser("org1@uit.edu.vn",   "Org12345",  "Le Van Cuong",     "0901111111", "UIT Campus, Thu Duc, TP.HCM", UserRole.ORGANIZER);
        User org2  = saveUser("org2@uit.edu.vn",   "Org12345",  "Pham Thi Dung",    "0902222222", "UIT Campus, Thu Duc, TP.HCM", UserRole.ORGANIZER);
        User cust1 = saveUser("cust1@uit.edu.vn",  "Cust1234",  "Nguyen Van A",     "0903333333", "123 Nguyen Van Cu, Q5, TP.HCM", UserRole.CUSTOMER);
        User cust2 = saveUser("cust2@uit.edu.vn",  "Cust1234",  "Tran Thi B",       "0904444444", "456 Le Van Viet, Q9, TP.HCM", UserRole.CUSTOMER);

        // ── Organizations ─────────────────────────────────────────────────────
        // org1 → ACTIVE  (can create + publish merch)
        // org2 → PENDING (waiting for admin approval — use admin account to approve)
        Organization devClub   = saveOrg(org1.getId(), "UIT Dev Club",   "Technology and software development club", OrganizationStatus.ACTIVE);
        Organization artStudio = saveOrg(org2.getId(), "UIT Art Studio", "Creative arts and design club",            OrganizationStatus.PENDING);

        // ── Merch ─────────────────────────────────────────────────────────────
        MerchItem hoodie = saveMerch(devClub.getId(), "UIT Hoodie",
                "Black premium hoodie with embroidered UIT logo", new BigDecimal("250000"), 50, MerchItemStatus.PUBLISHED);
        MerchItem tshirt = saveMerch(devClub.getId(), "UIT Classic T-Shirt",
                "White 100% cotton t-shirt with UIT screenprint",  new BigDecimal("150000"), 100, MerchItemStatus.PUBLISHED);
        MerchItem cap    = saveMerch(devClub.getId(), "UIT Baseball Cap",
                "Navy structured cap with UIT embroidery",          new BigDecimal("80000"),  30,  MerchItemStatus.PUBLISHED);
        MerchItem tote   = saveMerch(devClub.getId(), "UIT Tote Bag",
                "Heavy-duty canvas tote bag — coming soon",         new BigDecimal("70000"),  20,  MerchItemStatus.DRAFT);

        // ── Events ────────────────────────────────────────────────────────────
        Event techFair  = saveEvent(devClub.getId(), "Tech Fair 2026",
                "Annual technology showcase with student projects and industry guests",
                EventStatus.PUBLISHED,
                LocalDateTime.of(2026, 6, 1, 8, 0), LocalDateTime.of(2026, 6, 1, 17, 0));
        Event hackathon = saveEvent(devClub.getId(), "Hackathon 2026",
                "48-hour coding marathon — build, break, ship",
                EventStatus.DRAFT,
                LocalDateTime.of(2026, 7, 15, 8, 0), LocalDateTime.of(2026, 7, 17, 8, 0));

        attachMerchToEvent(techFair.getId(), hoodie.getId());
        attachMerchToEvent(techFair.getId(), tshirt.getId());

        // ── Wishlist for cust1 ────────────────────────────────────────────────
        Wishlist wishlist = wishlistRepository.save(Wishlist.builder().userId(cust1.getId()).build());
        wishlistItemRepository.save(WishlistItem.builder().wishlistId(wishlist.getId()).merchId(hoodie.getId()).build());
        wishlistItemRepository.save(WishlistItem.builder().wishlistId(wishlist.getId()).merchId(cap.getId()).build());

        // ── Cart for cust1 (active, not checked out) ─────────────────────────
        Cart cart = cartRepository.save(Cart.builder().userId(cust1.getId()).build());
        cartItemRepository.save(CartItem.builder().cartId(cart.getId()).merchId(hoodie.getId()).quantity(1).build());
        cartItemRepository.save(CartItem.builder().cartId(cart.getId()).merchId(tshirt.getId()).quantity(2).build());

        // ── Orders ────────────────────────────────────────────────────────────
        // cust1 — PENDING (organizer can confirm this)
        saveOrder(cust1.getId(), devClub.getId(), OrderStatus.PENDING, null,
                List.of(new Line(hoodie, 2)));

        // cust1 — CONFIRMED (organizer can move to READY_FOR_PICKUP)
        saveOrder(cust1.getId(), devClub.getId(), OrderStatus.CONFIRMED, "Please pack separately",
                List.of(new Line(tshirt, 3), new Line(cap, 1)));

        // cust2 — READY_FOR_PICKUP (organizer can move to SUCCESS)
        saveOrder(cust2.getId(), devClub.getId(), OrderStatus.READY_FOR_PICKUP, null,
                List.of(new Line(hoodie, 1)));

        // guest — PENDING
        saveGuestOrder(devClub.getId(),
                "Vo Thi C", "0905555555", "789 Vo Van Ngan, Thu Duc, TP.HCM", "guest@example.com",
                List.of(new Line(cap, 2)));

        log.info("Sample data ready — accounts:");
        log.info("  admin@uit.edu.vn  / Admin123  [ADMIN]");
        log.info("  org1@uit.edu.vn   / Org12345  [ORGANIZER — ACTIVE org, has merch + events + orders]");
        log.info("  org2@uit.edu.vn   / Org12345  [ORGANIZER — PENDING org, needs admin approval]");
        log.info("  cust1@uit.edu.vn  / Cust1234  [CUSTOMER  — has cart, wishlist, 2 orders]");
        log.info("  cust2@uit.edu.vn  / Cust1234  [CUSTOMER  — has 1 order READY_FOR_PICKUP]");
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private User saveUser(String email, String rawPassword, String fullName,
                          String phone, String address, UserRole role) {
        return userRepository.save(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .fullName(fullName)
                .phone(phone)
                .address(address)
                .role(role)
                .isVerified(true)
                .build());
    }

    private Organization saveOrg(UUID ownerId, String name, String description,
                                  OrganizationStatus status) {
        return organizationRepository.save(Organization.builder()
                .ownerId(ownerId)
                .name(name)
                .description(description)
                .status(status)
                .build());
    }

    private MerchItem saveMerch(UUID orgId, String name, String description,
                                 BigDecimal price, int stock, MerchItemStatus status) {
        return merchItemRepository.save(MerchItem.builder()
                .orgId(orgId)
                .name(name)
                .description(description)
                .price(price)
                .stock(stock)
                .status(status)
                .build());
    }

    private Event saveEvent(UUID orgId, String title, String description,
                             EventStatus status, LocalDateTime startsAt, LocalDateTime endsAt) {
        return eventRepository.save(Event.builder()
                .orgId(orgId)
                .title(title)
                .description(description)
                .status(status)
                .startsAt(startsAt)
                .endsAt(endsAt)
                .build());
    }

    private void attachMerchToEvent(UUID eventId, UUID merchId) {
        eventMerchRepository.save(EventMerch.builder()
                .eventId(eventId)
                .merchId(merchId)
                .build());
    }

    private void saveOrder(UUID userId, UUID orgId, OrderStatus status, String note,
                            List<Line> lines) {
        BigDecimal total = lines.stream()
                .map(l -> l.merch().getPrice().multiply(BigDecimal.valueOf(l.qty())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = orderRepository.save(Order.builder()
                .userId(userId)
                .orgId(orgId)
                .totalAmount(total)
                .status(status)
                .paymentMethod(PaymentMethod.CASH_ON_DELIVERY)
                .paymentStatus(PaymentStatus.PENDING)
                .note(note)
                .build());

        lines.forEach(l -> saveOrderItem(order.getId(), l));
    }

    private void saveGuestOrder(UUID orgId, String guestName, String guestPhone,
                                 String guestAddress, String guestEmail, List<Line> lines) {
        BigDecimal total = lines.stream()
                .map(l -> l.merch().getPrice().multiply(BigDecimal.valueOf(l.qty())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = orderRepository.save(Order.builder()
                .orgId(orgId)
                .guestName(guestName)
                .guestPhone(guestPhone)
                .guestAddress(guestAddress)
                .guestEmail(guestEmail)
                .totalAmount(total)
                .status(OrderStatus.PENDING)
                .paymentMethod(PaymentMethod.CASH_ON_DELIVERY)
                .paymentStatus(PaymentStatus.PENDING)
                .build());

        lines.forEach(l -> saveOrderItem(order.getId(), l));
    }

    private void saveOrderItem(UUID orderId, Line l) {
        orderItemRepository.save(OrderItem.builder()
                .orderId(orderId)
                .merchId(l.merch().getId())
                .merchName(l.merch().getName())
                .unitPrice(l.merch().getPrice())
                .quantity(l.qty())
                .subtotal(l.merch().getPrice().multiply(BigDecimal.valueOf(l.qty())))
                .build());
    }

    private record Line(MerchItem merch, int qty) {}
}
