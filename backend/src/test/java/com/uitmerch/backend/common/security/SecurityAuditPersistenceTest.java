package com.uitmerch.backend.common.security;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import java.sql.DriverManager;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@EnabledIfEnvironmentVariable(named="UITMERCH_TEST_DATABASE_URL", matches="jdbc:postgresql:.*")
class SecurityAuditPersistenceTest {
    @Test void persistsRedactedOutcomesDeniesBrowserRolesPrunesOldEventsAndSurvivesStorageFailure() throws Exception {
        String base=System.getenv("UITMERCH_TEST_DATABASE_URL");
        String name="uitmerch_audit_"+UUID.randomUUID().toString().replace("-", "");
        try (var connection=DriverManager.getConnection(base,"postgres","uitmerch_test_only"); var admin=connection.createStatement()) {
            admin.execute("DO $$ DECLARE r text; BEGIN FOREACH r IN ARRAY ARRAY['anon','authenticated','service_role'] LOOP IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname=r) THEN EXECUTE format('CREATE ROLE %I',r); END IF; END LOOP; END $$");
            admin.execute("CREATE DATABASE "+name);
            try {
                var jdbc=new JdbcTemplate(new DriverManagerDataSource(base.substring(0,base.lastIndexOf('/')+1)+name,"postgres","uitmerch_test_only"));
                String migration=new String(getClass().getResourceAsStream("/db/migration/V50__durable_security_audit.sql").readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
                jdbc.execute(migration);
                var metrics=new SimpleMeterRegistry();
                var store=new SecurityAuditStore(jdbc,metrics,90);
                @SuppressWarnings("unchecked") ObjectProvider<SecurityAuditStore> provider=mock(ObjectProvider.class);
                when(provider.getIfAvailable()).thenReturn(store);
                var request=new MockHttpServletRequest("POST","/api/v1/auth/NEVER_LOG_PATH_SECRET");
                request.setQueryString("token=NEVER_LOG_TOKEN");request.setContent("NEVER_LOG_PASSWORD".getBytes());
                request.addHeader("Cookie","NEVER_LOG_COOKIE");
                var response=new MockHttpServletResponse();response.setHeader("X-Trace-Id","NEVER_LOG_TRACE\nsecret");
                UUID actor=UUID.randomUUID();
                new SecurityAuditFilter(provider).doFilter(request,response,(req,res)->{request.setAttribute("userId",actor);response.setStatus(403);});
                var row=jdbc.queryForMap("SELECT * FROM security_audit.events");
                assertThat(row.get("action")).isEqualTo("/api/v1/auth/{unmatched}");
                assertThat(row.get("actor_id")).isEqualTo(actor);
                assertThat(((Number)row.get("status")).intValue()).isEqualTo(403);
                assertThat(row.get("trace_id")).isNull();
                assertThat(row.toString()).doesNotContain("NEVER_LOG");
                assertThat(jdbc.queryForObject("SELECT relrowsecurity FROM pg_class WHERE oid='security_audit.events'::regclass",Boolean.class)).isTrue();
                for(String role: new String[]{"anon","authenticated","service_role"}) {
                    if(Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM pg_roles WHERE rolname=?)",Boolean.class,role))) {
                        assertThat(jdbc.queryForObject("SELECT has_schema_privilege(?, 'security_audit', 'USAGE')",Boolean.class,role)).isFalse();
                        assertThat(jdbc.queryForObject("SELECT has_table_privilege(?, 'security_audit.events', 'SELECT')",Boolean.class,role)).isFalse();
                    }
                }
                store.record("POST","/api/v1/auth/login",null,200,"trace-safe");
                jdbc.update("UPDATE security_audit.events SET occurred_at=now()-interval '91 days' WHERE actor_id=?",actor);
                store.pruneExpired();
                assertThat(jdbc.queryForObject("SELECT count(*) FROM security_audit.events",Integer.class)).isEqualTo(1);
                assertThat(metrics.get("uitmerch.security.audit.pruned").counter().count()).isEqualTo(1);
                jdbc.execute("DROP TABLE security_audit.events");
                assertThatCode(()->store.record("POST","/api/v1/auth/login",null,403,null)).doesNotThrowAnyException();
                assertThat(metrics.get("uitmerch.security.audit.persistence.failures").counter().count()).isEqualTo(1);
            } finally { admin.execute("DROP DATABASE "+name+" WITH (FORCE)"); }
        }
    }
}
