package com.uitmerch.backend.performance;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.flywaydb.core.Flyway;
import org.flywaydb.database.postgresql.PostgreSQLConfigurationExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Verify automatic planning and record the forced-generic case without dictating its chosen index. */
@EnabledIfEnvironmentVariable(named = "UITMERCH_TEST_DATABASE_URL", matches = "jdbc:postgresql://127\\.0\\.0\\.1:.*")
class PreparedSearchIndexTest {
    @Test
    void preparedSearchUsesIndexWithAutomaticPlanning() throws Exception {
        String base = System.getenv("UITMERCH_TEST_DATABASE_URL");
        String name = "uitmerch_search_" + UUID.randomUUID().toString().replace("-", "");
        String url = base.substring(0, base.lastIndexOf('/') + 1) + name;
        try (var admin = DriverManager.getConnection(base, "postgres", "uitmerch_test_only"); var ddl = admin.createStatement()) {
            ddl.execute("CREATE DATABASE " + name);
            try {
                var configuration = Flyway.configure().dataSource(url, "postgres", "uitmerch_test_only");
                configuration.getPluginRegister().getPlugin(PostgreSQLConfigurationExtension.class).setTransactionalLock(false);
                configuration.load().migrate();
                try (var db = DriverManager.getConnection(url, "postgres", "uitmerch_test_only"); var sql = db.createStatement()) {
                    sql.executeUpdate("""
                        INSERT INTO merch_items(id,org_id,name,price,stock,status)
                        SELECT gen_random_uuid(),(SELECT id FROM organizations WHERE status='ACTIVE' LIMIT 1),
                               'Optimization product '||g,1000,10,'PUBLISHED'
                        FROM generate_series(1,30000) g
                        """);
                    sql.execute("VACUUM (ANALYZE) merch_items");
                    sql.execute("ANALYZE organizations");
                    sql.execute("SET plan_cache_mode=auto");
                    sql.execute("""
                        PREPARE optimization_search AS
                        SELECT m.* FROM merch_items m WHERE m.status=$1
                        AND lower(m.name) LIKE lower(concat('%',$2::text,'%'))
                        AND EXISTS (SELECT o.id FROM organizations o WHERE o.id=m.org_id AND o.status=$3)
                        ORDER BY m.created_at DESC OFFSET $4 ROWS FETCH FIRST $5 ROWS ONLY
                        """);
                    String execute = "EXECUTE optimization_search('PUBLISHED','product 29999','ACTIVE',0,20)";
                    // Cross the threshold at which auto planning considers a generic plan.
                    for (int i = 0; i < 8; i++) {
                        try (var rows = sql.executeQuery(execute)) {
                            assertThat(rows.next()).isTrue();
                            assertThat(rows.getString("name")).isEqualTo("Optimization product 29999");
                            assertThat(rows.next()).isFalse();
                        }
                    }
                    ObjectMapper json = new ObjectMapper();
                    var reportData = json.createObjectNode();
                    reportData.put("inserted_merch_rows", 30000);
                    try (var plan = sql.executeQuery("EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON) " + execute)) {
                        assertThat(plan.next()).isTrue();
                        String value = plan.getString(1);
                        reportData.set("auto_plan", json.readTree(value));
                        assertThat(value).contains("idx_merch_name_trgm");
                    }
                    sql.execute("SET plan_cache_mode=force_generic_plan");
                    try (var plan = sql.executeQuery("EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON) " + execute)) {
                        assertThat(plan.next()).isTrue();
                        reportData.set("forced_generic_plan", json.readTree(plan.getString(1)));
                    }
                    try (var rows = sql.executeQuery(execute)) {
                        assertThat(rows.next()).isTrue();
                        assertThat(rows.getString("name")).isEqualTo("Optimization product 29999");
                        assertThat(rows.next()).isFalse();
                    }
                    Path report = Path.of("target/optimization-index-plan.json");
                    Files.createDirectories(report.getParent());
                    Files.writeString(report, json.writerWithDefaultPrettyPrinter().writeValueAsString(reportData));
                }
            } finally { ddl.execute("DROP DATABASE " + name + " WITH (FORCE)"); }
        }
    }
}
