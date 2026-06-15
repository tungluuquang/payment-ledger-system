package org.vippro.audit_service.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.vippro.audit_service.model.AuditEvent;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.sql.Timestamp;

@Repository
public class AuditEventStore {

    private static final String POSTGRES_INSERT = """
            INSERT INTO audit_events (
                event_id, payment_id, correlation_id, event_type,
                source_topic, source_partition, source_offset,
                trace_id, span_id, payload, content_hash,
                occurred_at, recorded_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (event_id) DO NOTHING
            """;

    private static final String H2_INSERT = """
            INSERT INTO audit_events (
                event_id, payment_id, correlation_id, event_type,
                source_topic, source_partition, source_offset,
                trace_id, span_id, payload, content_hash,
                occurred_at, recorded_at
            )
            SELECT ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
            WHERE NOT EXISTS (
                SELECT 1 FROM audit_events WHERE event_id = ?
            )
            """;

    private final JdbcTemplate jdbcTemplate;
    private final boolean h2;

    public AuditEventStore(
            JdbcTemplate jdbcTemplate,
            DataSource dataSource
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.h2 = isH2(dataSource);
    }

    public boolean insertIfAbsent(AuditEvent event) {
        Object[] parameters = {
                event.getEventId(),
                event.getPaymentId(),
                event.getCorrelationId(),
                event.getEventType(),
                event.getSourceTopic(),
                event.getSourcePartition(),
                event.getSourceOffset(),
                event.getTraceId(),
                event.getSpanId(),
                event.getPayload(),
                event.getContentHash(),
                Timestamp.from(event.getOccurredAt()),
                Timestamp.from(event.getRecordedAt())
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
                        parameters[9],
                        parameters[10],
                        parameters[11],
                        parameters[12],
                        event.getEventId()
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
                    "Could not determine audit database product",
                    e
            );
        }
    }
}
