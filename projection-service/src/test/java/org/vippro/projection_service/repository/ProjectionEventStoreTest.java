package org.vippro.projection_service.repository;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.DatabaseMetaData;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectionEventStoreTest {

    @Test
    void bindsInstantsAsSqlTimestampsForPostgres() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        DataSource dataSource = mock(DataSource.class);
        var connection = mock(java.sql.Connection.class);
        DatabaseMetaData metadata = mock(DatabaseMetaData.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metadata);
        when(metadata.getDatabaseProductName()).thenReturn("PostgreSQL");
        doReturn(1).when(jdbcTemplate).update(
                anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
        Instant occurredAt = Instant.parse("2026-06-15T01:00:00Z");
        Instant processedAt = occurredAt.plusSeconds(1);
        UUID eventId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();

        boolean inserted = new ProjectionEventStore(
                jdbcTemplate,
                dataSource
        ).insertIfAbsent(
                eventId,
                paymentId,
                correlationId,
                "PaymentInitiated",
                "{}",
                occurredAt,
                processedAt
        );

        assertTrue(inserted);
        verify(jdbcTemplate).update(
                anyString(),
                eq(eventId),
                eq(paymentId),
                eq(correlationId),
                eq("PaymentInitiated"),
                eq("{}"),
                eq(Timestamp.from(occurredAt)),
                eq(Timestamp.from(processedAt))
        );
    }
}
