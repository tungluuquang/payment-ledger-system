package org.vippro.fraud_check_service.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

@Repository
public class ProcessedCommandStore {

    private static final String POSTGRES_INSERT = """
            INSERT INTO processed_commands (
                command_id,
                command_type,
                processed_at
            )
            VALUES (?, ?, ?)
            ON CONFLICT (command_id) DO NOTHING
            """;

    private static final String H2_INSERT = """
            INSERT INTO processed_commands (
                command_id,
                command_type,
                processed_at
            )
            SELECT ?, ?, ?
            WHERE NOT EXISTS (
                SELECT 1 FROM processed_commands WHERE command_id = ?
            )
            """;

    private final JdbcTemplate jdbcTemplate;
    private final boolean h2;

    public ProcessedCommandStore(
            JdbcTemplate jdbcTemplate,
            DataSource dataSource
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.h2 = isH2(dataSource);
    }

    public boolean insertIfAbsent(
            UUID commandId,
            String commandType,
            Instant processedAt
    ) {
        int inserted = h2
                ? jdbcTemplate.update(
                        H2_INSERT,
                        commandId,
                        commandType,
                        Timestamp.from(processedAt),
                        commandId
                )
                : jdbcTemplate.update(
                        POSTGRES_INSERT,
                        commandId,
                        commandType,
                        Timestamp.from(processedAt)
                );
        return inserted == 1;
    }

    private boolean isH2(DataSource dataSource) {
        try (var connection = dataSource.getConnection()) {
            return connection.getMetaData()
                    .getDatabaseProductName()
                    .equalsIgnoreCase("H2");
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Could not determine database product",
                    e
            );
        }
    }
}
