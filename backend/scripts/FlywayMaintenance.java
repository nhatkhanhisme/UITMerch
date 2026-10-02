import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cdimascio.dotenv.Dotenv;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.zip.CRC32;

/** Explicit maintenance only: never invoked by application startup. */
public class FlywayMaintenance {
    public static void main(String[] args) throws Exception {
        String action = args.length == 0 ? "validate" : args[0];
        if (!Set.of("validate", "repair-v16", "migrate").contains(action)) {
            throw new IllegalArgumentException("Use validate, repair-v16, or migrate");
        }
        var env = Dotenv.configure().ignoreIfMissing().load();
        var ds = new DriverManagerDataSource(env.get("SPRING_DATASOURCE_URL"),
                env.get("SPRING_DATASOURCE_USERNAME"), env.get("SPRING_DATASOURCE_PASSWORD"));
        var properties = new Properties();
        properties.setProperty("connectTimeout", "10");
        properties.setProperty("socketTimeout", "120");
        if (action.equals("validate")) properties.setProperty("options", "-c default_transaction_read_only=on");
        ds.setConnectionProperties(properties);
        var flyway = Flyway.configure().dataSource(ds).locations("filesystem:src/main/resources/db/migration")
                .ignoreMigrationPatterns("*:pending").load();
        if (action.equals("repair-v16")) {
            var validation = flyway.validateWithResult();
            if (validation.validationSuccessful) {
                System.out.println("Schema history already validates; no repair performed.");
                return;
            }
            if (validation.invalidMigrations.size() != 1 ||
                    !"16".equals(validation.invalidMigrations.getFirst().version) ||
                    !"CHECKSUM_MISMATCH".equals(validation.invalidMigrations.getFirst().errorDetails.errorCode.toString())) {
                throw new IllegalStateException("Repair refused: mismatch is not limited to the known V16 semicolon fix.");
            }
            var checksum = new CRC32();
            for (String line : Files.readAllLines(Path.of("src/main/resources/db/migration/V16__seed_five_real_events.sql"), StandardCharsets.UTF_8)) {
                checksum.update(line.getBytes(StandardCharsets.UTF_8));
            }
            if ((int) checksum.getValue() != 1114743976) {
                throw new IllegalStateException("Repair refused: resolved V16 differs from the reviewed semicolon fix.");
            }
            try (var db = ds.getConnection(); var statement = db.createStatement()) {
                var history = new ArrayList<Map<String, Object>>();
                try (var rows = statement.executeQuery("SELECT version, script, checksum, success FROM flyway_schema_history ORDER BY installed_rank")) {
                    while (rows.next()) {
                        var row = new LinkedHashMap<String, Object>();
                        row.put("version", rows.getString(1)); row.put("script", rows.getString(2));
                        row.put("checksum", rows.getObject(3)); row.put("success", rows.getBoolean(4));
                        history.add(row);
                    }
                }
                var v16 = history.stream().filter(row -> "16".equals(row.get("version"))).findFirst().orElseThrow();
                if (!Integer.valueOf(42806920).equals(v16.get("checksum")) ||
                        !"V16__seed_five_real_events.sql".equals(v16.get("script")) || !Boolean.TRUE.equals(v16.get("success"))) {
                    throw new IllegalStateException("Repair refused: applied V16 is not the known deployed revision.");
                }
                Path backup = Path.of(env.get("UITMERCH_MIGRATION_BACKUP_DIR", "/tmp"));
                Files.createDirectories(backup);
                Path file = Files.createTempFile(backup, "uitmerch-flyway-history-", ".json");
                new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(file.toFile(), history);
                System.out.println("Schema history backup: " + file.toAbsolutePath());
            }
            flyway.repair();
            flyway.validate();
            System.out.println("Known V16 checksum repaired; application tables were not changed.");
        } else if (action.equals("migrate")) {
            flyway.validate();
            var result = flyway.migrate();
            flyway.validate();
            System.out.println("Applied migrations: " + result.migrationsExecuted + "; schema version: " + result.targetSchemaVersion);
        } else {
            var result = flyway.validateWithResult();
            System.out.println("Validation: " + result.validationSuccessful);
            for (var migration : result.invalidMigrations) {
                System.out.println("Invalid V" + migration.version + ": " + migration.errorDetails.errorCode);
            }
            for (var migration : flyway.info().pending()) System.out.println("Pending V" + migration.getVersion());
            if (!result.validationSuccessful) System.exit(1);
        }
    }
}
