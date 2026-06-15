package org.vippro.fraud_check_service.repository;

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

class ProcessedCommandStoreTest {

    @Test
    void bindsInstantAsSqlTimestampForPostgres() throws Exception {
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
                org.mockito.ArgumentMatchers.any()
        );
        Instant processedAt = Instant.parse("2026-06-15T01:00:00Z");
        UUID commandId = UUID.randomUUID();

        boolean inserted = new ProcessedCommandStore(
                jdbcTemplate,
                dataSource
        ).insertIfAbsent(commandId, "FraudCheckRequestedCommand", processedAt);

        assertTrue(inserted);
        verify(jdbcTemplate).update(
                anyString(),
                eq(commandId),
                eq("FraudCheckRequestedCommand"),
                eq(Timestamp.from(processedAt))
        );
    }
}
