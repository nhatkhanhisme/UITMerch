package com.uitmerch.backend.ai;

import com.uitmerch.backend.ai.service.EmbeddingService;
import com.uitmerch.backend.ai.service.MerchEmbeddingService;
import com.uitmerch.backend.common.model.MerchItemStatus;
import com.uitmerch.backend.merch.entity.MerchItem;
import com.uitmerch.backend.merch.repository.MerchItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
@Profile("!(dev | docker)")
@RequiredArgsConstructor
public class MerchEmbeddingBackfillRunner {

    private final MerchItemRepository merchItemRepository;
    private final MerchEmbeddingService merchEmbeddingService;
    private final EmbeddingService embeddingService;

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void backfill() {
        Set<UUID> alreadyEmbedded = new HashSet<>(merchEmbeddingService.findAllMerchIdsWithEmbedding());

        List<MerchItem> toProcess = merchItemRepository.findAllByStatus(MerchItemStatus.PUBLISHED)
            .stream()
            .filter(m -> !alreadyEmbedded.contains(m.getId()))
            .toList();

        if (toProcess.isEmpty()) {
            log.info("Embedding backfill: all {} published items already embedded.", alreadyEmbedded.size());
            return;
        }

        log.info("Embedding backfill: {} items to process.", toProcess.size());
        int success = 0;
        for (MerchItem item : toProcess) {
            try {
                String text = item.getName() +
                    (item.getDescription() != null ? " " + item.getDescription() : "");
                float[] vec = embeddingService.embed(text);
                merchEmbeddingService.store(item.getId(), vec);
                success++;
            } catch (Exception e) {
                log.warn("Backfill failed for merch {}: {}", item.getId(), e.getMessage());
            }
        }
        log.info("Embedding backfill complete: {}/{} items embedded.", success, toProcess.size());
    }
}
