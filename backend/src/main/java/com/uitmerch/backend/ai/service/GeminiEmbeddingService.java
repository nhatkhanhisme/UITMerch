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
        "https://generativelanguage.googleapis.com/v1/models/gemini-embedding-001:embedContent?key=";

    private final GeminiKeyRotator rotator;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GeminiEmbeddingService(GeminiKeyRotator rotator, ObjectMapper objectMapper) {
        this.rotator = rotator;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
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
            float[] vec = new float[values.size()];
            for (int i = 0; i < vec.length; i++) {
                vec[i] = (float) values.get(i).asDouble();
            }
            return vec;

        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Gemini Embedding exception: {}", e.getMessage());
            throw new ValidationException("Embedding service unavailable.");
        }
    }

    private HttpResponse<String> callWithRotation(String requestBody) throws Exception {
        int attempts = rotator.keyCount();
        for (int i = 0; i < attempts; i++) {
            String key = (i == 0) ? rotator.currentKey() : rotator.rotateAndGet();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ENDPOINT + key))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 429) {
                log.warn("Gemini Embedding 429 on attempt {}/{}", i + 1, attempts);
                continue;
            }
            if (response.statusCode() != 200) {
                log.warn("Gemini Embedding API error: HTTP {} — {}", response.statusCode(), response.body());
                throw new ValidationException("Embedding service error: HTTP " + response.statusCode());
            }
            return response;
        }
        throw new ValidationException("All Gemini API keys are rate-limited (429). Try again later.");
    }
}
