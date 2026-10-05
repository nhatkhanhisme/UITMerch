package com.uitmerch.backend.order.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uitmerch.backend.auth.repository.UserRepository;
import com.uitmerch.backend.common.delivery.BackgroundJobService;
import com.uitmerch.backend.common.exception.*;
import com.uitmerch.backend.common.model.UserRole;
import com.uitmerch.backend.order.dto.OrderResponse;
import com.uitmerch.backend.order.entity.Order;
import com.uitmerch.backend.common.security.SecurityCredentials;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.SecureRandom;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor
public class CheckoutSecurityService {
    private final CheckoutActorRepository actors;
    private final CheckoutAttemptRepository attempts;
    private final UserRepository users;
    private final BackgroundJobService jobs;
    private final PasswordEncoder passwords;
    private final ObjectMapper json;
    private final JdbcTemplate jdbc;
    private final SecureRandom random = new SecureRandom();
    @Value("${app.checkout.max-quantity:10}") private int maxQuantity;
    @Value("${app.checkout.max-pending:3}") private int maxPending;
    @Value("${app.checkout.pending-hours:48}") private int pendingHours;

    @jakarta.annotation.PostConstruct
    void validateConfiguration() {
        if(maxQuantity<1 || maxPending<1 || pendingHours<1) throw new IllegalStateException("Checkout limits must be positive.");
    }
    public record Started(String id, String actor, List<OrderResponse> replay) {}
    public String email(String value) {
        if (value == null || value.isBlank() || value.length()>255) throw new ValidationException("A verified guest email is required.");
        return value.trim().toLowerCase(Locale.ROOT);
    }
    private CheckoutActor lockedActor(String key) {
        String dialect = jdbc.execute((org.springframework.jdbc.core.ConnectionCallback<String>) c -> c.getMetaData().getDatabaseProductName());
        if ("PostgreSQL".equals(dialect)) {
            jdbc.update("INSERT INTO checkout_actors(actor_key,failed_attempts) VALUES (?,0) ON CONFLICT DO NOTHING",key);
        } else {
            jdbc.update("MERGE INTO checkout_actors t USING (VALUES (?)) s(actor_key) ON t.actor_key=s.actor_key WHEN NOT MATCHED THEN INSERT(actor_key,failed_attempts) VALUES(s.actor_key,0)",key);
        }
        return actors.locked(key).orElseThrow();
    }
    @Transactional
    public UUID challenge(String rawEmail) {
        String email=email(rawEmail);
        var actor=lockedActor("guest:"+SecurityCredentials.hash(email));
        String code=String.format(Locale.ROOT,"%06d",random.nextInt(1_000_000));
        actor.setCodeHash(passwords.encode(code)); actor.setChallengeId(UUID.randomUUID());
        actor.setCodeExpiresAt(Instant.now().plusSeconds(600)); actor.setFailedAttempts(0);
        jobs.enqueueEmail("OTP",email,code);
        return actor.getChallengeId();
    }
    /** Invalid attempts commit their counters; controller reports the error after this transaction. */
    @Transactional
    public String verify(String rawEmail, UUID challenge, String code) {
        var actor=lockedActor("guest:"+SecurityCredentials.hash(email(rawEmail)));
        if (actor.getCodeHash()==null || !Objects.equals(challenge,actor.getChallengeId())
            || actor.getCodeExpiresAt()==null || !actor.getCodeExpiresAt().isAfter(Instant.now()) || actor.getFailedAttempts()>=5) return null;
        if (!passwords.matches(code,actor.getCodeHash())) { actor.setFailedAttempts(actor.getFailedAttempts()+1); return null; }
        String token=SecurityCredentials.generate();
        actor.setTokenHash(SecurityCredentials.hash(token));actor.setVerifiedUntil(Instant.now().plusSeconds(1800));
        actor.setCodeHash(null);actor.setCodeExpiresAt(null);
        return token;
    }
    public void quantities(Map<UUID,Integer> quantities) {
        if (quantities.isEmpty() || quantities.size()>100 || quantities.values().stream().anyMatch(q->q==null || q<1 || q>maxQuantity))
            throw new ValidationException("Quantity per product must be between 1 and "+maxQuantity+".");
    }
    public void availableProducts(Set<UUID> products) {
        String placeholders=String.join(",",Collections.nCopies(products.size(),"?"));
        Long eligible=jdbc.queryForObject("SELECT COUNT(*) FROM merch_items m JOIN organizations o ON o.id=m.org_id JOIN users owner ON owner.id=o.owner_id WHERE m.id IN ("+placeholders+") AND m.status='PUBLISHED' AND o.status='ACTIVE' AND owner.is_active=TRUE AND owner.is_verified=TRUE",Long.class,products.toArray());
        if(eligible==null || eligible!=products.size()) throw new ValidationException("Some products are no longer available for checkout.");
    }
    public Started begin(UUID user, String rawEmail, String token, UUID requestId, String kind, Object payload) {
        if (requestId==null) throw new ValidationException("requestId is required for checkout retries.");
        String buyerEmail;
        if (user!=null) {
            var account=users.findLockedById(user).filter(u->u.isActive() && u.isVerified() && u.getRole()==UserRole.CUSTOMER)
                .orElseThrow(()->new AuthenticationException("Invalid account"));
            buyerEmail=email(account.getEmail());
        } else buyerEmail=email(rawEmail);
        String key="guest:"+SecurityCredentials.hash(buyerEmail);
        var actor=lockedActor(key);
        if (user==null && (token==null || token.length()!=43 || actor.getTokenHash()==null
            || actor.getVerifiedUntil()==null || !actor.getVerifiedUntil().isAfter(Instant.now())
            || !SecurityCredentials.matches(token,actor.getTokenHash()))) throw new AuthenticationException("Verify your email before checkout.");
        String id=SecurityCredentials.hash((user==null?key:"user:"+user)+":"+requestId);
        try {
            String fingerprint=SecurityCredentials.hash(kind+":"+json.writeValueAsString(payload));
            var previous=attempts.findById(id);
            if (previous.isPresent()) {
                var p=previous.get();
                if (!p.getFingerprint().equals(fingerprint)) throw new AppException("requestId belongs to a different checkout.",HttpStatus.CONFLICT,"IDEMPOTENCY_CONFLICT");
                if (p.getResponseJson()==null) throw new AppException("Checkout is still being processed.",HttpStatus.CONFLICT,"CHECKOUT_PROCESSING");
                return new Started(id,key,json.readValue(p.getResponseJson(),new TypeReference<List<OrderResponse>>(){}));
            }
            Long count=jdbc.queryForObject("SELECT COUNT(DISTINCT COALESCE(checkout_id,CAST(id AS VARCHAR))) FROM orders WHERE status='PENDING' AND (checkout_actor=? OR (user_id IS NOT NULL AND CAST(user_id AS VARCHAR)=?) OR (user_id IS NULL AND LOWER(TRIM(guest_email))=?))",Long.class,key,user==null?"":user.toString(),buyerEmail);
            if (count!=null && count>=maxPending) throw new ValidationException("You already have "+maxPending+" pending checkouts.");
            var attempt=new CheckoutAttempt();attempt.setId(id);attempt.setActorKey(key);attempt.setFingerprint(fingerprint);
            attempt.setCreatedAt(Instant.now());attempts.saveAndFlush(attempt);
            return new Started(id,key,null);
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) { throw new IllegalStateException("Unable to persist checkout result",ex); }
    }
    public void attach(Order order, Started checkout, boolean campaign) {
        order.setCheckoutId(checkout.id()); order.setCheckoutActor(checkout.actor());
        if (!campaign) order.setPendingExpiresAt(Instant.now().plus(Duration.ofHours(pendingHours)));
    }
    public void finish(Started checkout,List<OrderResponse> orders) {
        try { attempts.findById(checkout.id()).orElseThrow().setResponseJson(json.writeValueAsString(orders)); }
        catch (com.fasterxml.jackson.core.JsonProcessingException ex) { throw new IllegalStateException("Unable to persist checkout result",ex); }
    }
}
