package com.uitmerch.backend.common.delivery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
public class BackgroundJobService {
    private final BackgroundJobRepository repository;
    private final ObjectMapper json;
    public record MailPayload(String template, String recipient, List<String> arguments) {}

    @Transactional
    public void enqueueAnnouncement(UUID id) { enqueue("ANNOUNCEMENT", id.toString()); }

    @Transactional
    public void enqueueEmail(String template, String recipient, String... arguments) {
        try {
            enqueue("EMAIL", json.writeValueAsString(new MailPayload(template, recipient, Arrays.asList(arguments))));
        } catch (JsonProcessingException e) { throw new IllegalStateException("Unable to queue email", e); }
    }

    @Transactional
    public void enqueueEmbedding(UUID merchId) {
        enqueue("EMBEDDING", merchId.toString());
    }

    private void enqueue(String kind, String payload) {
        BackgroundJob job = new BackgroundJob();
        job.setKind(kind); job.setPayload(payload);
        repository.save(job);
    }
}
