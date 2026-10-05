package com.uitmerch.backend.features;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.sql.*;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

/** Exercises an actual deployed V34 upgrade in a separate disposable database. */
@EnabledIfEnvironmentVariable(named="UITMERCH_TEST_DATABASE_URL",matches="jdbc:postgresql:.*")
class MigrationUpgradeFeatureTest {
    @Test void upgradesBugFixSchemaWithoutChangingStockOrSendingHistoricalPublicationAlerts() throws Exception {
        String base=System.getenv("UITMERCH_TEST_DATABASE_URL");
        String name="uitmerch_upgrade_"+UUID.randomUUID().toString().replace("-","");
        String upgrade=base.substring(0,base.lastIndexOf('/')+1)+name;
        try(Connection admin=DriverManager.getConnection(base,"postgres","uitmerch_test_only")) {
            try(Statement sql=admin.createStatement()) { sql.execute("DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname='anon') THEN CREATE ROLE anon; END IF; IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname='authenticated') THEN CREATE ROLE authenticated; END IF; END $$;"); sql.execute("CREATE DATABASE "+name); }
            try {
                Flyway old=Flyway.configure().dataSource(upgrade,"postgres","uitmerch_test_only").target("34").load(); old.migrate();
                long stock;
                try(Connection db=DriverManager.getConnection(upgrade,"postgres","uitmerch_test_only");Statement sql=db.createStatement()) {
                    try(ResultSet rs=sql.executeQuery("SELECT to_regclass('auth_sessions') IS NULL, to_regclass('background_jobs') IS NULL")){rs.next();assertThat(rs.getBoolean(1)).isTrue();assertThat(rs.getBoolean(2)).isTrue();}
                    try(ResultSet rs=sql.executeQuery("SELECT purpose FROM otp_tokens LIMIT 1")){if(rs.next())assertThat(rs.getString(1)).isEqualTo("VERIFY_EMAIL");}
                }
                try(Connection db=DriverManager.getConnection(upgrade,"postgres","uitmerch_test_only");Statement sql=db.createStatement()) {
                    try(ResultSet rs=sql.executeQuery("SELECT SUM(stock) FROM merch_items")){rs.next();stock=rs.getLong(1);}
                }
                var configuration=Flyway.configure().dataSource(upgrade,"postgres","uitmerch_test_only");
                configuration.getPluginRegister().getPlugin(org.flywaydb.database.postgresql.PostgreSQLConfigurationExtension.class)
                    .setTransactionalLock(false);
                Flyway current=configuration.load();
                assertThat(current.migrate().migrationsExecuted).isEqualTo(16); current.validate();
                assertThat(current.info().current().getVersion().getVersion()).isEqualTo("50");
                try(Connection db=DriverManager.getConnection(upgrade,"postgres","uitmerch_test_only");Statement sql=db.createStatement()) {
                    try(ResultSet rs=sql.executeQuery("SELECT SUM(stock) FROM merch_items")){rs.next();assertThat(rs.getLong(1)).isEqualTo(stock);}
                    try(ResultSet rs=sql.executeQuery("SELECT COUNT(*) FROM merch_items WHERE status='PUBLISHED' AND NOT publication_announced")){rs.next();assertThat(rs.getLong(1)).isZero();}
                    try(ResultSet rs=sql.executeQuery("SELECT COUNT(*) FROM events WHERE status<>'DRAFT' AND NOT publication_announced")){rs.next();assertThat(rs.getLong(1)).isZero();}
                    try(ResultSet rs=sql.executeQuery("SELECT COUNT(*) FROM pg_tables WHERE schemaname='public' AND NOT rowsecurity")){rs.next();assertThat(rs.getLong(1)).isZero();}
                    try(ResultSet rs=sql.executeQuery("SELECT COUNT(*) FROM pg_extension e JOIN pg_namespace n ON n.oid=e.extnamespace WHERE extname IN ('vector','pg_trgm') AND nspname='extensions'")){rs.next();assertThat(rs.getLong(1)).isEqualTo(2);}
                    sql.execute("SET search_path TO public, extensions");
                    try(ResultSet rs=sql.executeQuery("SELECT '[1,0]'::vector <=> '[1,0]'::vector, similarity('merch','merch')")){rs.next();assertThat(rs.getDouble(1)).isZero();assertThat(rs.getDouble(2)).isEqualTo(1);}
                    try(ResultSet rs=sql.executeQuery("SELECT COUNT(*) FROM pg_index WHERE NOT indisvalid")){rs.next();assertThat(rs.getLong(1)).isZero();}
                    try(ResultSet rs=sql.executeQuery("SELECT has_table_privilege('anon','orders','SELECT'), has_table_privilege('authenticated','users','SELECT')")){rs.next();assertThat(rs.getBoolean(1)).isFalse();assertThat(rs.getBoolean(2)).isFalse();}
                    try(ResultSet rs=sql.executeQuery("SELECT COUNT(*) FROM announcement_events")){rs.next();assertThat(rs.getLong(1)).isZero();}
                    try(ResultSet rs=sql.executeQuery("SELECT COUNT(*) FROM users WHERE password_hash = '$2a$10$bJMeGHBL0q8J4kceezsn7uR48Kcm45xd2UpNwzJzJkJCh2e7hJPqe' AND is_active")){rs.next();assertThat(rs.getLong(1)).isZero();}
                }
            } finally {
                try(Statement sql=admin.createStatement()){sql.execute("DROP DATABASE "+name+" WITH (FORCE)");}
            }
        }
    }
}
