package com.uitmerch.backend.common.security;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.*;
import static org.assertj.core.api.Assertions.*;
class SecurityAuditFilterTest {
    @Test void recordsSecurityOutcomesWithoutCredentialsBodiesOrQueryStrings() throws Exception {
        Logger logger=(Logger)LoggerFactory.getLogger("uitmerch.security.audit");
        var appender=new ListAppender<ILoggingEvent>();appender.start();logger.addAppender(appender);
        try {
            var request=new MockHttpServletRequest("POST","/api/v1/auth/login");
            request.addHeader("Authorization","Bearer NEVER_LOG_AUTH");request.addHeader("Cookie","NEVER_LOG_COOKIE");
            request.setQueryString("token=NEVER_LOG_QUERY");request.setContent("NEVER_LOG_PASSWORD".getBytes());
            var response=new MockHttpServletResponse();response.setHeader("X-Trace-Id","audit-trace");
            var provider=org.mockito.Mockito.mock(org.springframework.beans.factory.ObjectProvider.class);
            new SecurityAuditFilter(provider).doFilter(request,response,(req,res)->{request.setAttribute("userId","00000000-0000-0000-0000-000000000001");response.setStatus(403);});
            assertThat(appender.list).hasSize(1);
            assertThat(appender.list.getFirst().getFormattedMessage()).contains("actor=00000000-0000-0000-0000-000000000001","status=403","trace=audit-trace")
                .doesNotContain("NEVER_LOG", "token=");
        } finally {logger.detachAppender(appender);appender.stop();}
    }
}
