package com.uitmerch.backend.features;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.sql.*;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

/** Exercises an actual V34 -> V41 upgrade in a separate disposable database. */
@EnabledIfEnvironmentVariable(named="UITMERCH_TEST_DATABASE_URL",matches="jdbc:postgresql:.*")
class MigrationUpgradeFeatureTest {
    @Test void upgradesBugFixSchemaWithoutChangingStockOrSendingHistoricalPublicationAlerts() throws Exception {
        String base=System.getenv("UITMERCH_TEST_DATABASE_URL");
        String name="uitmerch_upgrade_"+UUID.randomUUID().toString().replace("-","");
        String upgrade=base.substring(0,base.lastIndexOf('/')+1)+name;
        try(Connection admin=DriverManager.getConnection(base,"postgres","uitmerch_test_only")) {
            try(Statement sql=admin.createStatement()) { sql.execute("CREATE DATABASE "+name); }
            try {
                Flyway old=Flyway.configure().dataSource(upgrade,"postgres","uitmerch_test_only").target("34").load(); old.migrate();
                long stock;
                try(Connection db=DriverManager.getConnection(upgrade,"postgres","uitmerch_test_only");Statement sql=db.createStatement()) {
                    try(ResultSet rs=sql.executeQuery("SELECT SUM(stock) FROM merch_items")){rs.next();stock=rs.getLong(1);}
                }
                Flyway current=Flyway.configure().dataSource(upgrade,"postgres","uitmerch_test_only").load();
                assertThat(current.migrate().migrationsExecuted).isEqualTo(7); current.validate();
                assertThat(current.info().current().getVersion().getVersion()).isEqualTo("41");
                try(Connection db=DriverManager.getConnection(upgrade,"postgres","uitmerch_test_only");Statement sql=db.createStatement()) {
                    try(ResultSet rs=sql.executeQuery("SELECT SUM(stock) FROM merch_items")){rs.next();assertThat(rs.getLong(1)).isEqualTo(stock);}
                    try(ResultSet rs=sql.executeQuery("SELECT COUNT(*) FROM merch_items WHERE status='PUBLISHED' AND NOT publication_announced")){rs.next();assertThat(rs.getLong(1)).isZero();}
                    try(ResultSet rs=sql.executeQuery("SELECT COUNT(*) FROM events WHERE status<>'DRAFT' AND NOT publication_announced")){rs.next();assertThat(rs.getLong(1)).isZero();}
                    try(ResultSet rs=sql.executeQuery("SELECT COUNT(*) FROM announcement_events")){rs.next();assertThat(rs.getLong(1)).isZero();}
                }
            } finally {
                try(Statement sql=admin.createStatement()){sql.execute("DROP DATABASE "+name+" WITH (FORCE)");}
            }
        }
    }
}
