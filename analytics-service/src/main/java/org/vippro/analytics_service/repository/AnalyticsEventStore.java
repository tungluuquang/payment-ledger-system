package org.vippro.analytics_service.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.vippro.analytics_service.service.AnalyticsEventMetadata;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.sql.Timestamp;

@Repository
public class AnalyticsEventStore {

    private static final String POSTGRES_INSERT = """
            INSERT INTO analytics_events (
                event_id, payment_id, correlation_id, event_type,
                event_category, error_code, reason, occurred_at, processed_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (event_id) DO NOTHING
            """;

    private static final String H2_INSERT = """
            INSERT INTO analytics_events (
                event_id, payment_id, correlation_id, event_type,
                event_category, error_code, reason, occurred_at, processed_at
            )
            SELECT ?, ?, ?, ?, ?, ?, ?, ?, ?
            WHERE NOT EXISTS (
                SELECT 1 FROM analytics_events WHERE event_id = ?
            )
            """;

    private final JdbcTemplate jdbcTemplate;
    private final boolean h2;

    public AnalyticsEventStore(
            JdbcTemplate jdbcTemplate,
            DataSource dataSource
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.h2 = isH2(dataSource);
    }

    public boolean insertIfAbsent(
            AnalyticsEventMetadata metadata,
            String eventType,
            java.time.Instant processedAt
    ) {
        Object[] parameters = {
                metadata.eventId(),
                metadata.paymentId(),
                metadata.correlationId(),
                eventType,
                metadata.category(),
                metadata.errorCode(),
                metadata.reason(),
                Timestamp.from(metadata.occurredAt()),
                Timestamp.from(processedAt)
        };
        int inserted = h2
                ? jdbcTemplate.update(
                        H2_INSERT,
                        parameters[0],
                        parameters[1],
                        parameters[2],
                        parameters[3],
                        parameters[4],
                        parameters[5],
                        parameters[6],
                        parameters[7],
                        parameters[8],
                        metadata.eventId()
                )
                : jdbcTemplate.update(POSTGRES_INSERT, parameters);
        return inserted == 1;
    }

    private boolean isH2(DataSource dataSource) {
        try (var connection = dataSource.getConnection()) {
            return connection.getMetaData()
                    .getDatabaseProductName()
                    .equalsIgnoreCase("H2");
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Could not determine analytics database product",
                    e
            );
        }
    }
}
