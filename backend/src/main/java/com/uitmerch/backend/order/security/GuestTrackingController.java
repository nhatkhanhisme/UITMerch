package com.uitmerch.backend.order.security;
import com.uitmerch.backend.common.model.ApiResponse;
import com.uitmerch.backend.common.exception.RateLimitException;
import com.uitmerch.backend.common.service.RateLimiterService;
import com.uitmerch.backend.common.util.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.time.Duration;
import java.util.UUID;
@RestController @RequiredArgsConstructor
public class GuestTrackingController {
    private final GuestTrackingService tracking;
    private final RateLimiterService rates;
    private final IpUtil ips;
    @PostMapping("/api/v1/public/orders/{id}/tracking-receipt")
    public ResponseEntity<?> request(@PathVariable UUID id,@Valid @RequestBody GuestCheckoutController.EmailRequest request,HttpServletRequest http) {
        if (!rates.isAllowed("tracking-mail-ip:"+ips.extractClientIp(http),5,Duration.ofMinutes(15))
            || !rates.isAllowed("tracking-mail-order:"+id,3,Duration.ofMinutes(15))) throw new RateLimitException("Too many tracking requests.",900);
        tracking.request(id,request.email());
        return ResponseEntity.accepted().cacheControl(CacheControl.noStore())
            .body(ApiResponse.success("If the details match, a tracking code was emailed.",null));
    }
    @GetMapping("/api/v1/public/orders/{id}/tracking")
    public ResponseEntity<?> read(@PathVariable UUID id,@RequestHeader(value="X-Guest-Tracking",required=false) String token,HttpServletRequest http) {
        if (!rates.isAllowed("tracking-ip:"+ips.extractClientIp(http),30,Duration.ofMinutes(1))) throw new RateLimitException("Too many tracking requests.",60);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success("Order retrieved.",tracking.read(id,token)));
    }
}
