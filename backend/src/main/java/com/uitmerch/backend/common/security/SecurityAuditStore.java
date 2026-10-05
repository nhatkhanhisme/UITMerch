package com.uitmerch.backend.common.security;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.UUID;

/** Durable security metadata only. No request bodies, credentials, emails or IP addresses. */
@Component
@org.springframework.context.annotation.Lazy(false)
@Profile("!dev")
public class SecurityAuditStore {
    private static final Logger log = LoggerFactory.getLogger(SecurityAuditStore.class);
    private final JdbcTemplate jdbc;
    private final MeterRegistry metrics;
    private final int retentionDays;

    public SecurityAuditStore(JdbcTemplate jdbc, MeterRegistry metrics,
            @Value("${app.audit.retention-days:90}") int retentionDays) {
        if (retentionDays < 1 || retentionDays > 365) throw new IllegalArgumentException("Audit retention must be 1–365 days");
        this.jdbc = new JdbcTemplate(jdbc.getDataSource());
        this.jdbc.setQueryTimeout(3);
        this.metrics = metrics;
        this.retentionDays = retentionDays;
    }

    public void record(String method, String action, UUID actor, int status, String trace) {
        try {
            jdbc.update("INSERT INTO security_audit.events(id,method,action,actor_id,status,trace_id) VALUES (?,?,?,?,?,?)",
                UUID.randomUUID(), method, action, actor, status, trace);
            metrics.counter("uitmerch.security.audit.persisted").increment();
        } catch (org.springframework.dao.DataAccessException failure) {
            // The stdout event remains available on Render. Never expose SQL/connection details.
            metrics.counter("uitmerch.security.audit.persistence.failures").increment();
            log.error("security_audit_persistence_failed");
        }
    }

    @Scheduled(initialDelayString="${app.audit.cleanup-initial-delay-ms:60000}",
               fixedDelayString="${app.audit.cleanup-delay-ms:3600000}")
    public void pruneExpired() {
        try {
            int removed = jdbc.update("""
                WITH expired AS (
                    SELECT id FROM security_audit.events
                    WHERE occurred_at < now() - (? * interval '1 day')
                    ORDER BY occurred_at, id LIMIT 5000 FOR UPDATE SKIP LOCKED
                ) DELETE FROM security_audit.events e USING expired WHERE e.id=expired.id
                """, retentionDays);
            metrics.counter("uitmerch.security.audit.pruned").increment(removed);
        } catch (org.springframework.dao.DataAccessException failure) {
            metrics.counter("uitmerch.security.audit.retention.failures").increment();
            log.error("security_audit_retention_failed");
        }
    }
}
