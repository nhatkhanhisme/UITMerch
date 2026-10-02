package com.uitmerch.backend.campaign;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.Instant;
@Component @RequiredArgsConstructor @Slf4j
@ConditionalOnProperty(name="app.campaigns.enabled",havingValue="true",matchIfMissing=true)
public class CampaignScheduler {
    private final CampaignRepository campaigns;
    private final CampaignService service;
    @Scheduled(fixedDelayString="${app.campaigns.poll-ms:60000}")
    public void closeDueCampaigns() {
        for(var id:campaigns.due(Instant.now(),PageRequest.of(0,50))) {
            try { service.finalizeDue(id); }
            catch(Exception failure) { log.warn("Campaign finalization failed for {} ({})",id,failure.getClass().getSimpleName()); }
        }
    }
}
