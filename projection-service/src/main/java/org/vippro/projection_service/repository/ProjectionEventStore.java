package org.vippro.projection_service.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;

@Repository
public class ProjectionEventStore {

    private static final String POSTGRES_INSERT = """
            INSERT INTO projection_events (
                event_id,
                payment_id,
                correlation_id,
                event_type,
                payload,
                occurred_at,
                processed_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (event_id) DO NOTHING
            """;

    private static final String H2_INSERT = """
            INSERT INTO projection_events (
                event_id,
                payment_id,
                correlation_id,
                event_type,
                payload,
                occurred_at,
                processed_at
            )
            SELECT ?, ?, ?, ?, ?, ?, ?
            WHERE NOT EXISTS (
                SELECT 1 FROM projection_events WHERE event_id = ?
            )
            """;

    private final JdbcTemplate jdbcTemplate;
    private final boolean h2;

    public ProjectionEventStore(
            JdbcTemplate jdbcTemplate,
            DataSource dataSource
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.h2 = isH2(dataSource);
    }

    public boolean insertIfAbsent(
            UUID eventId,
            UUID paymentId,
            UUID correlationId,
            String eventType,
            String payload,
            Instant occurredAt,
            Instant processedAt
    ) {
        int inserted = h2
                ? jdbcTemplate.update(
                        H2_INSERT,
                        eventId,
                        paymentId,
                        correlationId,
                        eventType,
                        payload,
                        occurredAt,
                        processedAt,
                        eventId
                )
                : jdbcTemplate.update(
                        POSTGRES_INSERT,
                        eventId,
                        paymentId,
                        correlationId,
                        eventType,
                        payload,
                        occurredAt,
                        processedAt
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
