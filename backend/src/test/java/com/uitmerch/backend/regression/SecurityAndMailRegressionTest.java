package com.uitmerch.backend.regression;

import com.uitmerch.backend.common.security.JwtTokenProvider;
import com.uitmerch.backend.common.service.JavaMailEmailService;
import com.uitmerch.backend.common.util.ImageContentValidator;
import com.uitmerch.backend.common.exception.ValidationException;
import com.uitmerch.backend.ai.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.mail.javamail.JavaMailSender;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.mockito.ArgumentCaptor;
import java.util.Properties;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SecurityAndMailRegressionTest {
    private JwtTokenProvider jwt() {
        var jwt = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwt, "secretKey", "uitmerch-disposable-regression-test-secret-2026");
        ReflectionTestUtils.setField(jwt, "accessTokenExpiration", 3600000L);
        ReflectionTestUtils.setField(jwt, "refreshTokenExpiration", 604800000L);
        return jwt;
    }
    @Test void tokensAreUniqueEvenWhenIssuedInTheSameSecond() {
        var provider = jwt();
        assertThat(provider.generateRefreshToken("user")).isNotEqualTo(provider.generateRefreshToken("user"));
        String token = provider.generateSessionAccessToken("user", "test@uit.edu.vn", "CUSTOMER", "session", 7);
        assertThat(provider.validateAsAccessToken(token)).isTrue();
        assertThat(provider.getAuthVersionFromToken(token)).isEqualTo(7);
        assertThat(provider.getSessionIdFromToken(token)).isEqualTo("session");
        assertThat(provider.validateAsAccessToken(provider.generateRefreshToken("user"))).isFalse();
    }
    @Test void wrongIssuerIsRejectedEvenWithAValidSignature() {
        String token = io.jsonwebtoken.Jwts.builder().issuer("other-app").claim("type", "access")
            .expiration(new java.util.Date(System.currentTimeMillis() + 60000))
            .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor("uitmerch-disposable-regression-test-secret-2026".getBytes(java.nio.charset.StandardCharsets.UTF_8)))
            .compact();
        assertThat(jwt().validateToken(token)).isFalse();
    }
    @Test void pickupAndCancellationMailEscapeUserSuppliedHtml() throws Exception {
        var sender = mock(JavaMailSender.class);
        when(sender.createMimeMessage()).thenAnswer(inv -> new MimeMessage(Session.getInstance(new Properties())));
        var mail = new JavaMailEmailService(sender);
        ReflectionTestUtils.setField(mail, "fromAddress", "test@uit.edu.vn"); ReflectionTestUtils.setField(mail, "fromName", "UITMerch");
        mail.sendPickupScheduleNotification("test@uit.edu.vn", "12345678-abcd", "<date>", "<slot>", "<img src=x onerror=bad>", "<script>alert(1)</script>");
        mail.sendOrderCancelledNotification("test@uit.edu.vn", "12345678-abcd", "<a href=bad>Click</a>", "customer");
        var captured = ArgumentCaptor.forClass(MimeMessage.class); verify(sender, times(2)).send(captured.capture());
        for (MimeMessage message : captured.getAllValues()) {
            message.saveChanges();
            String body = decodedBody(message.getContent());
            assertThat(body).contains("&lt;").doesNotContain("<img", "<script", "<a href=bad>");
        }
    }
    @Test void mislabeledAndOversizedImageContentIsRejected() throws Exception {
        assertThatThrownBy(() -> ImageContentValidator.validate("not an image".getBytes(), "image/png")).isInstanceOf(ValidationException.class);
        var output = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_RGB), "png", output);
        ImageContentValidator.validate(output.toByteArray(), "image/png");
        assertThatThrownBy(() -> ImageContentValidator.validate(output.toByteArray(), "image/jpeg")).isInstanceOf(ValidationException.class);
    }
    @Test void namedProductionProfileProvidesBothAiClients() {
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
            .withPropertyValues("spring.profiles.active=production")
            .withBean(GeminiKeyRotator.class, () -> mock(GeminiKeyRotator.class))
            .withBean(com.fasterxml.jackson.databind.ObjectMapper.class, com.fasterxml.jackson.databind.ObjectMapper::new)
            .withUserConfiguration(GeminiVisionAiService.class, GeminiEmbeddingService.class)
            .run(context -> {
                assertThat(context).hasSingleBean(VisionAiService.class).hasSingleBean(EmbeddingService.class);
            });
    }
    private String decodedBody(Object content) throws Exception {
        if (content instanceof String text) return text;
        if (content instanceof jakarta.mail.Multipart multipart) {
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < multipart.getCount(); i++) result.append(decodedBody(multipart.getBodyPart(i).getContent()));
            return result.toString();
        }
        return "";
    }
    @Test void imageDimensionsAreBoundedBeforeDecoding() throws Exception {
        var output = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(4097, 1, java.awt.image.BufferedImage.TYPE_INT_RGB), "png", output);
        assertThatThrownBy(() -> ImageContentValidator.validate(output.toByteArray(), "image/png")).isInstanceOf(ValidationException.class);
        byte[] webp = new byte[30];
        System.arraycopy("RIFF".getBytes(), 0, webp, 0, 4); webp[4] = 22;
        System.arraycopy("WEBPVP8X".getBytes(), 0, webp, 8, 8); webp[16] = 10;
        webp[25] = 0x20; // Width = 8193; valid envelope, oversized canvas.
        assertThatThrownBy(() -> ImageContentValidator.validate(webp, "image/webp")).isInstanceOf(ValidationException.class);
    }
}
