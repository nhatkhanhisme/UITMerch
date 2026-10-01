package com.uitmerch.backend.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uitmerch.backend.common.exception.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Profile("!(dev | docker)")
public class GeminiEmbeddingService implements EmbeddingService {

    private static final String ENDPOINT =
        "https://generativelanguage.googleapis.com/v1/models/gemini-embedding-001:embedContent";

    private final GeminiKeyRotator rotator;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GeminiEmbeddingService(GeminiKeyRotator rotator, ObjectMapper objectMapper) {
        this.rotator = rotator;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(5)).build();
    }

    @Override
    public float[] embed(String text) {
        if (!rotator.hasKeys()) {
            throw new ValidationException("GEMINI_API_KEY not configured.");
        }
        try {
            String requestBody = objectMapper.writeValueAsString(Map.of(
                "model", "models/gemini-embedding-001",
                "content", Map.of("parts", List.of(Map.of("text", text))),
                "outputDimensionality", 768
            ));
            log.debug("Gemini Embedding request: {}", requestBody);

            HttpResponse<String> response = callWithRotation(requestBody);

            JsonNode values = objectMapper.readTree(response.body()).at("/embedding/values");
            if (!values.isArray() || values.size() != 768) {
                throw new ValidationException("Embedding service returned an invalid vector.");
            }
            float[] vec = new float[values.size()];
            for (int i = 0; i < vec.length; i++) {
                if (!values.get(i).isNumber()) throw new ValidationException("Invalid embedding value.");
                vec[i] = (float) values.get(i).asDouble();
                if (!Float.isFinite(vec[i])) throw new ValidationException("Invalid embedding value.");
            }
            return vec;

        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            log.warn("Gemini Embedding request failed: {}", e.getClass().getSimpleName());
            throw new ValidationException("Embedding service unavailable.");
        }
    }

    private HttpResponse<String> callWithRotation(String requestBody) throws Exception {
        int attempts = Math.min(rotator.keyCount(), 3);
        java.time.Instant deadline = java.time.Instant.now().plusSeconds(20);
        for (int i = 0; i < attempts; i++) {
            java.time.Duration remaining = java.time.Duration.between(java.time.Instant.now(), deadline);
            if (remaining.isNegative() || remaining.isZero()) throw new ValidationException("AI service timed out.");
            String key = (i == 0) ? rotator.currentKey() : rotator.rotateAndGet();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ENDPOINT))
                .timeout(remaining)
                .header("x-goog-api-key", key)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 403) {
                rotator.markCurrentBad();
                log.warn("Gemini Embedding HTTP 403 on attempt {}/{} — key revoked, rotating", i + 1, attempts);
                continue;
            }
            if (response.statusCode() == 429) {
                log.warn("Gemini Embedding HTTP 429 on attempt {}/{} — rate limited, rotating", i + 1, attempts);
                continue;
            }
            if (response.statusCode() != 200) {
                log.warn("Gemini Embedding API error: HTTP {}", response.statusCode());
                throw new ValidationException("Embedding service error: HTTP " + response.statusCode());
            }
            return response;
        }
        throw new ValidationException("All Gemini API keys are rate-limited (429). Try again later.");
    }
}
