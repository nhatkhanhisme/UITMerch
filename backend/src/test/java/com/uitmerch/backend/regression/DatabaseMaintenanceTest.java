package com.uitmerch.backend.regression;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties={"app.jwt.secret=uitmerch-disposable-regression-test-secret-2026", "app.delivery.enabled=false", "app.checkout.expiry-enabled=false"})
@ActiveProfiles("docker")
@EnabledIfEnvironmentVariable(named="UITMERCH_TEST_DATABASE_URL", matches="jdbc:postgresql:.*")
class DatabaseMaintenanceTest {
    @DynamicPropertySource static void database(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", () -> System.getenv("UITMERCH_TEST_DATABASE_URL"));
        r.add("spring.datasource.username", () -> "postgres");
        r.add("spring.datasource.password", () -> "uitmerch_test_only");
    }
    @Autowired JdbcTemplate jdbc;
    @Autowired TransactionTemplate tx;

    @Test void expiryIsAtomicIdempotentPreservesConfirmedAndHistoricalOrdersAndQueuesRestock() {
        tx.executeWithoutResult(status -> {
            UUID customer=UUID.randomUUID(), owner=UUID.randomUUID(), org=UUID.randomUUID(), merch=UUID.randomUUID();
            for(UUID u: new UUID[]{customer,owner}) jdbc.update("INSERT INTO users(id,email,password_hash,full_name,role,is_verified) VALUES (?,?,?,'Maintenance test','CUSTOMER',true)",u,u+"@example.invalid","disabled");
            jdbc.update("INSERT INTO organizations(id,owner_id,name,status) VALUES (?,?,'Maintenance test','ACTIVE')",org,owner);
            jdbc.update("INSERT INTO merch_items(id,org_id,name,price,stock,status) VALUES (?,?,'Maintenance test',10000,0,'PUBLISHED')",merch,org);
            UUID expired=UUID.randomUUID(), confirmed=UUID.randomUUID(), historical=UUID.randomUUID(), future=UUID.randomUUID();
            for(UUID o:new UUID[]{expired,confirmed,historical,future}) {
                jdbc.update("INSERT INTO orders(id,user_id,org_id,total_amount,status,pending_expires_at) VALUES (?,?,?,10000,?::order_status,?)",o,customer,org,o.equals(confirmed)?"CONFIRMED":"PENDING",
                    o.equals(historical)?null:java.sql.Timestamp.from(java.time.Instant.now().plusSeconds(o.equals(future)?3600:-3600)));
                jdbc.update("INSERT INTO order_items(id,order_id,merch_id,merch_name,quantity,unit_price,subtotal) VALUES (?,?,?,'Maintenance test',2,5000,10000)",UUID.randomUUID(),o,merch);
            }
            assertThat(jdbc.queryForObject("SELECT app_ops.expire_pending_orders()",Integer.class)).isGreaterThanOrEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT stock FROM merch_items WHERE id=?",Integer.class,merch)).isEqualTo(2);
            assertThat(jdbc.queryForObject("SELECT status::text FROM orders WHERE id=?",String.class,expired)).isEqualTo("CANCELLED");
            assertThat(jdbc.queryForObject("SELECT count(*) FROM order_history WHERE order_id=? AND source='SYSTEM'",Integer.class,expired)).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM notifications WHERE related_order_id=?",Integer.class,expired)).isEqualTo(2);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM announcement_events WHERE merch_id=?",Integer.class,merch)).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM background_jobs j JOIN announcement_events e ON j.payload=e.id::text WHERE e.merch_id=?",Integer.class,merch)).isEqualTo(1);
            jdbc.queryForObject("SELECT app_ops.expire_pending_orders()",Integer.class);
            assertThat(jdbc.queryForObject("SELECT stock FROM merch_items WHERE id=?",Integer.class,merch)).isEqualTo(2);
            for(UUID o: new UUID[]{confirmed,historical,future}) assertThat(jdbc.queryForObject("SELECT status::text FROM orders WHERE id=?",String.class,o)).isEqualTo(o.equals(confirmed)?"CONFIRMED":"PENDING");
            status.setRollbackOnly();
        });
    }

    @Test void runtimeCanWriteBusinessDataAndAppendAuditButCannotDeleteAuditMigrateOrReadOperations() {
        tx.executeWithoutResult(status -> {
            jdbc.execute("SET LOCAL ROLE uitmerch_runtime");
            assertThat(jdbc.queryForObject("SELECT count(*) FROM merch_items",Integer.class)).isNotNull();
            UUID event=UUID.randomUUID();
            jdbc.update("INSERT INTO security_audit.events(id,method,action,status) VALUES (?,'POST','/api/v1/auth/login',401)",event);
            assertThat(jdbc.queryForObject("SELECT has_table_privilege(current_user,'security_audit.events','DELETE')",Boolean.class)).isFalse();
            assertThat(jdbc.queryForObject("SELECT has_table_privilege(current_user,'public.flyway_schema_history','UPDATE')",Boolean.class)).isFalse();
            assertThat(jdbc.queryForObject("SELECT has_schema_privilege(current_user,'public','CREATE')",Boolean.class)).isFalse();
            assertThat(jdbc.queryForObject("SELECT has_schema_privilege(current_user,'app_ops','USAGE')",Boolean.class)).isFalse();
            assertThat(jdbc.queryForObject("SELECT rolbypassrls OR rolsuper OR rolcreaterole FROM pg_roles WHERE rolname=current_user",Boolean.class)).isFalse();
            jdbc.execute("RESET ROLE"); status.setRollbackOnly();
        });
    }

    @Test void databaseRetentionRemovesOnlyExpiredAuditRows() {
        tx.executeWithoutResult(status -> {
            UUID old=UUID.randomUUID(), recent=UUID.randomUUID();
            jdbc.update("INSERT INTO security_audit.events(id,occurred_at,method,action,status) VALUES (?,now()-interval '91 days','POST','test',200)",old);
            jdbc.update("INSERT INTO security_audit.events(id,method,action,status) VALUES (?,'POST','test',200)",recent);
            jdbc.queryForObject("SELECT app_ops.prune_security_audit()",Integer.class);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM security_audit.events WHERE id=?",Integer.class,old)).isZero();
            assertThat(jdbc.queryForObject("SELECT count(*) FROM security_audit.events WHERE id=?",Integer.class,recent)).isEqualTo(1);
            status.setRollbackOnly();
        });
    }
    @Test void runtimeJobFailuresAreTimestampedAndMonitorReadsOnlyFixedHealthAggregates() {
        tx.executeWithoutResult(status -> {
            UUID id=UUID.randomUUID();
            jdbc.execute("SET LOCAL ROLE uitmerch_runtime");
            jdbc.update("INSERT INTO background_jobs(id,kind,payload) VALUES (?,'EMBEDDING','test')",id);
            jdbc.update("UPDATE background_jobs SET state='DEAD' WHERE id=?",id);
            assertThat(jdbc.queryForObject("SELECT failed_at IS NOT NULL FROM background_jobs WHERE id=?",Boolean.class,id)).isTrue();
            jdbc.update("UPDATE background_jobs SET failed_at=now()-interval '2 hours' WHERE id=?",id);
            jdbc.execute("RESET ROLE");
            jdbc.execute("SET LOCAL ROLE uitmerch_monitor");
            assertThat(jdbc.queryForObject("SELECT (app_ops.health_report()->>'unresolvedDeadJobs')::integer",Integer.class)).isGreaterThanOrEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT has_table_privilege(current_user,'public.users','SELECT')",Boolean.class)).isFalse();
            assertThat(jdbc.queryForObject("SELECT has_function_privilege(current_user,'app_ops.expire_pending_orders()','EXECUTE')",Boolean.class)).isFalse();
            jdbc.execute("RESET ROLE");status.setRollbackOnly();
        });
    }

}
