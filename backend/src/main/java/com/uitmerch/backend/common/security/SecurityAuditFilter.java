package com.uitmerch.backend.common.security;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.slf4j.*;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Set;
/** Security outcomes only: never record cookies, headers, bodies, OTPs or query strings. */
@Component @Order(Ordered.HIGHEST_PRECEDENCE+100)
public class SecurityAuditFilter extends OncePerRequestFilter {
    private static final Logger log=LoggerFactory.getLogger("uitmerch.security.audit");
    private final org.springframework.beans.factory.ObjectProvider<SecurityAuditStore> store;
    public SecurityAuditFilter(org.springframework.beans.factory.ObjectProvider<SecurityAuditStore> store) { this.store=store; }
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws IOException,ServletException {
        boolean completed=false;
        try { chain.doFilter(request,response); completed=true; }
        finally {
            String path=request.getRequestURI();
            boolean mutation=Set.of("POST","PUT","PATCH","DELETE").contains(request.getMethod());
            if(mutation && path.startsWith("/api/v1/") && (path.startsWith("/api/v1/auth/") || path.startsWith("/api/v1/admin/")
                || path.startsWith("/api/v1/uploads/") || path.startsWith("/api/v1/public/checkout/")
                || path.contains("/orders/") || path.endsWith("/orders") || path.contains("/campaigns/") || path.endsWith("/campaigns"))) {
                Object actor=request.getAttribute("userId");
                Object route=request.getAttribute(org.springframework.web.servlet.HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
                // A route template contains placeholders, never user-supplied path segments.
                String safePath=route instanceof String template ? template : fallbackAction(path);
                String trace=response.getHeader("X-Trace-Id");
                if(trace!=null && !trace.matches("[A-Za-z0-9._-]{1,64}")) trace=null;
                java.util.UUID actorId=null;
                try { if(actor!=null) actorId=java.util.UUID.fromString(actor.toString()); } catch(IllegalArgumentException ignored) {}
                log.info("security_event method={} action={} actor={} status={} trace={}",request.getMethod(),safePath,
                    actorId==null?"anonymous":actorId,completed?response.getStatus():500,trace);
                var durable=store.getIfAvailable();
                if(durable!=null) durable.record(request.getMethod(),safePath,actorId,completed?response.getStatus():500,trace);
            }
        }
    }
    private static String fallbackAction(String path) {
        for(String category: Set.of("auth","admin","uploads","public","organizer","customer")) {
            String prefix="/api/v1/"+category+"/";
            if(path.startsWith(prefix)) return prefix+"{unmatched}";
        }
        return "/api/v1/{unmatched}";
    }
}
