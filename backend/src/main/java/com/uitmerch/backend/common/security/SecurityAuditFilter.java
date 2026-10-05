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
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws IOException,ServletException {
        boolean completed=false;
        try { chain.doFilter(request,response); completed=true; }
        finally {
            String path=request.getRequestURI();
            boolean mutation=Set.of("POST","PUT","PATCH","DELETE").contains(request.getMethod());
            if(mutation && path.startsWith("/api/v1/") && (path.startsWith("/api/v1/auth/") || path.startsWith("/api/v1/admin/")
                || path.startsWith("/api/v1/uploads/") || path.contains("/orders/") || path.contains("/campaigns/"))) {
                Object actor=request.getAttribute("userId");
                String safePath=path.replaceAll("[0-9a-fA-F]{8}(?:-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}","{id}").replaceAll("[\\r\\n\\t]","");
                log.info("security_event method={} action={} actor={} status={} trace={}",request.getMethod(),safePath,
                    actor==null?"anonymous":actor,completed?response.getStatus():500,response.getHeader("X-Trace-Id"));
            }
        }
    }
}
