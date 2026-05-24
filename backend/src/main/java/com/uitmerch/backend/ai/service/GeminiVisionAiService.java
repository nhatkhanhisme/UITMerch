package com.uitmerch.backend.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uitmerch.backend.common.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Profile("default")
@RequiredArgsConstructor
public class GeminiVisionAiService implements VisionAiService {

    private static final String ENDPOINT =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=";

    private static final String PROMPT =
        "Mô tả ngắn gọn sản phẩm trong ảnh: tên sản phẩm, màu sắc, loại hàng. Tối đa 20 từ bằng tiếng Việt.";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${app.ai.gemini-api-key:}")
    private String apiKey;

    @Override
    public String describeImage(byte[] imageBytes, String mimeType) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ValidationException("GEMINI_API_KEY not configured. Please set GEMINI_API_KEY environment variable.");
        }

        try {
            String base64 = Base64.getEncoder().encodeToString(imageBytes);

            Map<String, Object> body = Map.of(
                "contents", List.of(Map.of(
                    "parts", List.of(
                        Map.of("inlineData", Map.of("mimeType", mimeType, "data", base64)),
                        Map.of("text", PROMPT)
                    )
                ))
            );

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ENDPOINT + apiKey))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("Gemini Vision API error: HTTP {} - {}", response.statusCode(), response.body());
                throw new ValidationException("Vision API returned HTTP " + response.statusCode() + ". Check API key and rate limits.");
            }

            JsonNode root = objectMapper.readTree(response.body());
            return root.path("candidates").get(0)
                .path("content").path("parts").get(0)
                .path("text").asText("").trim();

        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Gemini Vision API exception: {}", e.getMessage());
            throw new ValidationException("Vision service error: " + e.getMessage());
        }
    }
}
