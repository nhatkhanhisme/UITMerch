package com.uitmerch.backend.common.config;

import com.uitmerch.backend.common.exception.ResourceNotFoundException;
import com.uitmerch.backend.common.service.RateLimiterService;
import com.uitmerch.backend.common.util.IpUtil;
import com.uitmerch.backend.order.controller.PublicOrderController;
import com.uitmerch.backend.order.dto.OrderResponse;
import com.uitmerch.backend.order.service.OrderService;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PublicOrderController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class})
class GuestOrderSecurityTest {
    @Autowired MockMvc mvc;
    @MockBean OrderService orders;
    @MockBean RateLimiterService rateLimiter;
    @MockBean IpUtil ipUtil;
    @MockBean JwtAuthenticationFilter jwtFilter;

    @BeforeEach void forwardAnonymousRequests() throws Exception {
        doAnswer(call -> {
            FilterChain chain = call.getArgument(2);
            chain.doFilter(call.getArgument(0), call.getArgument(1));
            return null;
        }).when(jwtFilter).doFilter(any(), any(), any());
    }

    @Test void guestCanTrackWithoutAuthentication() throws Exception {
        UUID id = UUID.randomUUID();
        mvc.perform(get("/api/v1/public/orders/" + id).param("email", "guest@example.com"))
            .andExpect(status().isNotFound());
        verifyNoInteractions(orders);
    }

    @Test void wrongEmailRemainsNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        mvc.perform(get("/api/v1/public/orders/" + id).param("email", "wrong@example.com"))
            .andExpect(status().isNotFound());
    }

    @Test void malformedOrderIdReturnsBadRequest() throws Exception {
        mvc.perform(get("/api/v1/public/orders/invalid").param("email", "guest@example.com"))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(orders);
    }

    @Test void errorDispatchCanRenderOriginalError() throws Exception {
        mvc.perform(get("/error").requestAttr("jakarta.servlet.error.status_code", 400)
                .with(request -> { request.setDispatcherType(DispatcherType.ERROR); return request; }))
            .andExpect(status().isBadRequest());
    }

    @Test void normalRequestsToErrorAndPrivateApisStillRequireAuthentication() throws Exception {
        mvc.perform(get("/error")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/customer/orders")).andExpect(status().isUnauthorized());
    }
}
