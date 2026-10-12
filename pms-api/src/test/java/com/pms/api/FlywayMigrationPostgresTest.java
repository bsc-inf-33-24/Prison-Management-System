package com.pms.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class FlywayMigrationPostgresTest {

    @Container
    private final PostgreSQLContainer postgres = new PostgreSQLContainer(
            DockerImageName.parse("postgres:17-alpine"));

    @Test
    void upgradesPreV12DatabaseAndPreservesLegacyCaseData() throws Exception {
        Flyway preV12 = flyway().target("11").load();
        preV12.migrate();
        preV12.validate();

        try (Connection connection = postgres.createConnection("")) {
            insertLegacyRows(connection);
        }

        Flyway current = flyway().load();
        current.migrate();
        current.validate();

        try (Connection connection = postgres.createConnection("")) {
            assertTables(connection, Set.of(
                    "incident_inmate", "cells", "cell_allocations", "cases", "crimes",
                    "case_crimes", "sentences", "sentence", "Case", "crime", "casecrime"));
            assertColumns(connection, "cells", Set.of("id", "block_name", "cell_number", "capacity",
                    "classification", "created_at", "updated_at"));
            assertColumns(connection, "cell_allocations", Set.of("id", "inmate_id", "cell_id",
                    "allocated_at", "created_at", "updated_at"));
            assertColumns(connection, "cases", Set.of("id", "inmate_id", "case_number", "court",
                    "case_start_date", "case_end_date", "status", "created_by", "created_at", "updated_at"));
            assertColumns(connection, "crimes", Set.of("id", "offence_name", "description", "category",
                    "created_at", "updated_at"));
            assertColumns(connection, "case_crimes", Set.of("case_id", "crime_id"));
            assertColumns(connection, "sentences", Set.of("sentence_id", "case_id", "inmate_id", "duration",
                    "start_date", "release_date", "status", "created_at", "updated_at"));
            assertForeignKey(connection, "cell_allocations", "inmate_id", "inmates", "id");
            assertForeignKey(connection, "cell_allocations", "cell_id", "cells", "id");
            assertForeignKey(connection, "cases", "inmate_id", "inmates", "id");
            assertForeignKey(connection, "cases", "created_by", "users", "user_id");
            assertForeignKey(connection, "case_crimes", "case_id", "cases", "id");
            assertForeignKey(connection, "case_crimes", "crime_id", "crimes", "id");
            assertForeignKey(connection, "sentences", "case_id", "cases", "id");
            assertForeignKey(connection, "sentences", "inmate_id", "inmates", "id");
            assertUniqueIndex(connection, "cells", Set.of("block_name", "cell_number"));
            assertUniqueIndex(connection, "cell_allocations", Set.of("inmate_id"));
            assertCheckConstraint(connection, "cells", "capacity > 0");
            assertThat(count(connection, "\"Case\"")).isEqualTo(1);
            assertThat(count(connection, "crime")).isEqualTo(1);
            assertThat(count(connection, "casecrime")).isEqualTo(1);
            assertThat(count(connection, "sentence")).isEqualTo(1);
        }
    }

    private FluentConfiguration flyway() {
        return Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration");
    }

    private void insertLegacyRows(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO inmate (full_name) VALUES ('Legacy inmate')");
            statement.executeUpdate("INSERT INTO \"Case\" (inmate_id, case_number) "
                    + "VALUES (1, 'LEGACY-CASE')");
            statement.executeUpdate("INSERT INTO crime (offence_name) VALUES ('Legacy offence')");
            statement.executeUpdate("INSERT INTO casecrime (case_id, crime_id) VALUES (1, 1)");
            statement.executeUpdate("INSERT INTO sentence (case_id, inmate_id) VALUES (1, 1)");
        }
    }

    private void assertTables(Connection connection, Set<String> expected) throws Exception {
        Set<String> tables = new HashSet<>();
        try (ResultSet result = connection.getMetaData().getTables(null, "public", "%", new String[]{"TABLE"})) {
            while (result.next()) {
                tables.add(result.getString("TABLE_NAME"));
            }
        }
        assertThat(tables).containsAll(expected);
    }

    private void assertColumns(Connection connection, String table, Set<String> expected) throws Exception {
        Set<String> columns = new HashSet<>();
        try (ResultSet result = connection.getMetaData().getColumns(null, "public", table, "%")) {
            while (result.next()) {
                columns.add(result.getString("COLUMN_NAME"));
            }
        }
        assertThat(columns).containsAll(expected);
    }

    private void assertForeignKey(Connection connection, String table, String column,
                                  String referencedTable, String referencedColumn) throws Exception {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet result = metadata.getImportedKeys(null, "public", table)) {
            while (result.next()) {
                if (column.equals(result.getString("FKCOLUMN_NAME"))
                        && referencedTable.equals(result.getString("PKTABLE_NAME"))
                        && referencedColumn.equals(result.getString("PKCOLUMN_NAME"))) {
                    return;
                }
            }
        }
        throw new AssertionError("Missing foreign key " + table + "." + column + " -> "
                + referencedTable + "." + referencedColumn);
    }

    private void assertUniqueIndex(Connection connection, String table, Set<String> expectedColumns)
            throws Exception {
        DatabaseMetaData metadata = connection.getMetaData();
        Map<String, Set<String>> columnsByIndex = new HashMap<>();
        try (ResultSet result = metadata.getIndexInfo(null, "public", table, true, false)) {
            while (result.next()) {
                String column = result.getString("COLUMN_NAME");
                if (column != null) {
                    columnsByIndex.computeIfAbsent(result.getString("INDEX_NAME"), ignored -> new HashSet<>())
                            .add(column.toLowerCase(Locale.ROOT));
                }
            }
        }
        assertThat(columnsByIndex.values()).anySatisfy(columns -> assertThat(columns).containsExactlyInAnyOrderElementsOf(expectedColumns));
    }

    private void assertCheckConstraint(Connection connection, String table, String expectedExpression)
            throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT pg_get_constraintdef(c.oid) "
                     + "FROM pg_constraint c JOIN pg_class t ON t.oid = c.conrelid "
                     + "WHERE t.relname = '" + table + "' AND c.contype = 'c'")) {
            while (result.next()) {
                if (result.getString(1).toLowerCase(Locale.ROOT).contains(expectedExpression)) {
                    return;
                }
            }
        }
        throw new AssertionError("Missing check constraint on " + table + ": " + expectedExpression);
    }

    private long count(Connection connection, String table) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT count(*) FROM " + table)) {
            result.next();
            return result.getLong(1);
        }
    }
}
