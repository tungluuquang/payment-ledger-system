package org.vippro.projection_service.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.UUID;

@Repository
public class PaymentLockStore {

    private final JdbcTemplate jdbcTemplate;
    private final boolean h2;

    public PaymentLockStore(
            JdbcTemplate jdbcTemplate,
            DataSource dataSource
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.h2 = isH2(dataSource);
    }

    public void lock(UUID paymentId) {
        if (h2) {
            jdbcTemplate.update(
                    "MERGE INTO projection_payment_locks KEY(payment_id) VALUES (?)",
                    paymentId
            );
        } else {
            jdbcTemplate.update("""
                    INSERT INTO projection_payment_locks (payment_id)
                    VALUES (?)
                    ON CONFLICT (payment_id) DO NOTHING
                    """, paymentId);
        }
        jdbcTemplate.queryForObject(
                """
                SELECT payment_id
                FROM projection_payment_locks
                WHERE payment_id = ?
                FOR UPDATE
                """,
                UUID.class,
                paymentId
        );
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
