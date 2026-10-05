package com.uitmerch.backend.order.security;
import com.uitmerch.backend.common.exception.*;
import com.uitmerch.backend.common.model.ApiResponse;
import com.uitmerch.backend.common.service.RateLimiterService;
import com.uitmerch.backend.common.security.SecurityCredentials;
import com.uitmerch.backend.common.util.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.time.Duration;
import java.util.*;

@RestController @RequestMapping("/api/v1/public/checkout") @RequiredArgsConstructor
public class GuestCheckoutController {
    private final CheckoutSecurityService checkout;
    private final RateLimiterService rates;
    private final IpUtil ips;
    public record EmailRequest(@NotBlank @Email @Size(max=255) String email) {}
    public record VerifyRequest(@NotBlank @Email @Size(max=255) String email,@NotNull UUID challengeId,
        @NotBlank @Pattern(regexp="[0-9]{6}") String code) {}
    private void limit(String action,String email,HttpServletRequest http,int count) {
        if (!rates.isAllowed(action+":ip:"+ips.extractClientIp(http),count*3,Duration.ofMinutes(15))
            || !rates.isAllowed(action+":email:"+SecurityCredentials.hash(checkout.email(email)),count,Duration.ofMinutes(15)))
            throw new RateLimitException("Too many checkout verification requests.",900);
    }
    @PostMapping("/challenge")
    public ResponseEntity<?> challenge(@Valid @RequestBody EmailRequest request,HttpServletRequest http) {
        limit("guest-challenge",request.email(),http,3);
        return ResponseEntity.accepted().cacheControl(CacheControl.noStore())
            .body(ApiResponse.success("Check your email for a verification code.",Map.of("challengeId",checkout.challenge(request.email()))));
    }
    @PostMapping("/verify")
    public ResponseEntity<?> verify(@Valid @RequestBody VerifyRequest request,HttpServletRequest http) {
        limit("guest-verify",request.email(),http,10);
        String token=checkout.verify(request.email(),request.challengeId(),request.code());
        if (token==null) throw new AuthenticationException("Invalid or expired checkout verification code.");
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .body(ApiResponse.success("Email verified.",Map.of("guestCheckoutToken",token)));
    }
}
