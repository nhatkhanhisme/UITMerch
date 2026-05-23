package com.uitmerch.backend.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uitmerch.backend.common.exception.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

@Slf4j
@Service
@Profile("!(dev | docker)")
public class GeminiVisionAiService implements VisionAiService {

    private static final String ENDPOINT =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=";

    private static final String PROMPT =
        "Liệt kê 3 từ khóa tiếng Việt ngắn gọn mô tả sản phẩm trong ảnh " +
        "(ví dụ: áo thun, xanh, UIT), cách nhau bằng dấu phẩy. " +
        "Chỉ trả về từ khóa, không giải thích.";

    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GeminiVisionAiService(@Value("${app.ai.gemini-api-key}") String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String describeImage(byte[] imageBytes, String mimeType) {
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        String body = """
            {
              "contents": [{
                "parts": [
                  { "text": "%s" },
                  { "inline_data": { "mime_type": "%s", "data": "%s" } }
                ]
              }]
            }
            """.formatted(PROMPT, mimeType, base64Image);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ENDPOINT + apiKey))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("Gemini Vision returned HTTP {}: {}", response.statusCode(), response.body());
                throw new ValidationException("Không thể phân tích ảnh. Vui lòng thử lại.");
            }

            JsonNode root = objectMapper.readTree(response.body());
            return root.at("/candidates/0/content/parts/0/text").asText().trim();

        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Gemini Vision error: {}", e.getMessage());
            throw new ValidationException("Không thể phân tích ảnh. Vui lòng thử lại.");
        }
    }
}
