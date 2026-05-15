package com.uitmerch.backend.common.config;

import com.uitmerch.backend.common.model.ApiResponse;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@Hidden
@RestController
public class HealthController {

    @GetMapping("/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        Map<String, Object> data = Map.of(
            "status", "UP",
            "app", "UITMerch Backend",
            "timestamp", Instant.now().toString(),
            "links", Map.of(
                "swagger", "/swagger-ui.html",
                "openapi", "/v3/api-docs",
                "frontend", "https://uitmerch.vercel.app"
            )
        );
        return ResponseEntity.ok(ApiResponse.success("UITMerch Backend is running", data));
    }
}
