package com.uitmerch.backend.common.delivery;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uitmerch.backend.ai.service.*;
import com.uitmerch.backend.common.service.EmailService;
import com.uitmerch.backend.merch.repository.MerchItemRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Instant;
import java.util.UUID;

@Service
public class BackgroundJobDispatcher {
    private static final Logger log = LoggerFactory.getLogger(BackgroundJobDispatcher.class);
    private final BackgroundJobClaimService claims;
    private final EmailService transport;
    private final EmbeddingService embedding;
    private final MerchEmbeddingService merchEmbedding;
    private final MerchItemRepository merch;
    private final ObjectMapper json;
    private final com.uitmerch.backend.notification.announcement.AnnouncementDispatcher announcements;

    public BackgroundJobDispatcher(BackgroundJobClaimService claims, @Qualifier("mailTransport") EmailService transport,
        EmbeddingService embedding, MerchEmbeddingService merchEmbedding, MerchItemRepository merch, ObjectMapper json,
        com.uitmerch.backend.notification.announcement.AnnouncementDispatcher announcements) {
        this.claims = claims; this.transport = transport; this.embedding = embedding;
        this.merchEmbedding = merchEmbedding; this.merch = merch; this.json = json;
        this.announcements = announcements;
    }

    // Claim and result transactions are separate; no database transaction is held while doing provider I/O.
    public boolean dispatchNext() {
        var claimed = claims.claim();
        if (claimed.isEmpty()) return false;
        BackgroundJob job = claimed.get();
        boolean success = false;
        try {
            switch (job.getKind()) {
                case "EMAIL" -> sendMail(job);
                case "ANNOUNCEMENT" -> announcements.deliverBatch(UUID.fromString(job.getPayload()));
                case "EMBEDDING" -> {
                    UUID id = UUID.fromString(job.getPayload());
                    var item = merch.findById(id);
                    if (item.isPresent()) {
                        String text = item.get().getName() + (item.get().getDescription() == null ? "" : " " + item.get().getDescription());
                        merchEmbedding.store(id, embedding.embed(text));
                    }
                }
                default -> throw new IllegalStateException("Unknown job kind");
            }
            success = true;
        } catch (Exception e) {
            log.warn("Background job {} attempt {} failed ({})", job.getId(), job.getAttempts(), e.getClass().getSimpleName());
        }
        claims.finish(job.getId(), job.getAttempts(), success);
        return true;
    }

    private void sendMail(BackgroundJob job) throws Exception {
        var mail = json.readValue(job.getPayload(), BackgroundJobService.MailPayload.class);
        var args = mail.arguments();
        if (("OTP".equals(mail.template()) || "RESET".equals(mail.template()))
            && job.getCreatedAt().plusSeconds(15 * 60).isBefore(Instant.now())) return;
        switch (mail.template()) {
            case "ANNOUNCEMENT" -> transport.sendAnnouncement(mail.recipient(), args.get(0), args.get(1));
            case "OTP" -> transport.sendOtp(mail.recipient(), args.get(0));
            case "RESET" -> transport.sendPasswordReset(mail.recipient(), args.get(0));
            case "PLACED" -> transport.sendOrderPlacedConfirmation(mail.recipient(), args.get(0));
            case "STATUS" -> transport.sendOrderStatusUpdate(mail.recipient(), args.get(0), args.get(1));
            case "PICKUP" -> transport.sendPickupScheduleNotification(mail.recipient(), args.get(0), args.get(1), args.get(2), args.get(3), args.get(4));
            case "CANCELLED" -> transport.sendOrderCancelledNotification(mail.recipient(), args.get(0), args.get(1), args.get(2));
            default -> throw new IllegalStateException("Unknown mail template");
        }
    }
}
