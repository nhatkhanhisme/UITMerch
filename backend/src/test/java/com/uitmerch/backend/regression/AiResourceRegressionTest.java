package com.uitmerch.backend.regression;

import com.uitmerch.backend.ai.service.*;
import com.uitmerch.backend.merch.service.MerchService;
import com.uitmerch.backend.common.exception.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import java.net.http.*;
import java.util.*;
import java.time.Duration;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class AiResourceRegressionTest {
    @Test void providerRequestsUseDeadlinesAndAtMostThreeAttempts() throws Exception {
        var rotator = mock(GeminiKeyRotator.class);
        when(rotator.hasKeys()).thenReturn(true); when(rotator.keyCount()).thenReturn(10);
        when(rotator.currentKey()).thenReturn("test-key"); when(rotator.rotateAndGet()).thenReturn("test-key");
        var http = mock(HttpClient.class);
        @SuppressWarnings("unchecked") HttpResponse<String> limited = mock(HttpResponse.class);
        when(limited.statusCode()).thenReturn(429);
        when(http.send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(limited);
        var vision = new GeminiVisionAiService(rotator, new ObjectMapper());
        ReflectionTestUtils.setField(vision, "httpClient", http);
        assertThatThrownBy(() -> vision.describeImage(new byte[] {1}, "image/png")).isInstanceOf(ValidationException.class);
        var requests = org.mockito.ArgumentCaptor.forClass(HttpRequest.class);
        verify(http, times(3)).send(requests.capture(), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
        for (HttpRequest request : requests.getAllValues()) {
            assertThat(request.timeout().orElseThrow()).isPositive().isLessThanOrEqualTo(Duration.ofSeconds(20));
            assertThat(request.uri().toString()).doesNotContain("test-key");
            assertThat(request.headers().firstValue("x-goog-api-key")).contains("test-key");
        }
    }
    @Test void concurrentSearchLimitRejectsWorkBeforeCallingTheProvider() throws Exception {
        var vision = mock(VisionAiService.class); var embedding = mock(EmbeddingService.class);
        var indexed = mock(MerchEmbeddingService.class); var merch = mock(MerchService.class);
        var entered = new java.util.concurrent.CountDownLatch(2); var release = new java.util.concurrent.CountDownLatch(1);
        when(vision.describeImage(any(), anyString())).thenAnswer(inv -> {
            entered.countDown(); if (!release.await(5, java.util.concurrent.TimeUnit.SECONDS)) throw new IllegalStateException("Test timed out");
            return "shirt";
        });
        when(merch.listPublished(anyString(), isNull(), any())).thenReturn(org.springframework.data.domain.Page.empty());
        var search = new VisualSearchService(vision, embedding, indexed, merch);
        var output = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_RGB), "png", output);
        var file = new MockMultipartFile("image", "image.png", "image/png", output.toByteArray());
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> search.search(file)); var second = executor.submit(() -> search.search(file));
            try {
                assertThat(entered.await(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
                assertThatThrownBy(() -> search.search(file)).isInstanceOf(RateLimitException.class);
            } finally { release.countDown(); }
            first.get(10, java.util.concurrent.TimeUnit.SECONDS); second.get(10, java.util.concurrent.TimeUnit.SECONDS);
        }
        verify(vision, times(2)).describeImage(any(), anyString());
    }
}
