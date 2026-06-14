package org.vippro.fraud_check_service.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.UUID;

@Repository
public class AccountLockStore {

    private final JdbcTemplate jdbcTemplate;
    private final boolean h2;

    public AccountLockStore(
            JdbcTemplate jdbcTemplate,
            DataSource dataSource
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.h2 = isH2(dataSource);
    }

    public void lock(UUID accountId) {
        if (h2) {
            jdbcTemplate.update(
                    "MERGE INTO fraud_account_locks KEY(account_id) VALUES (?)",
                    accountId
            );
        } else {
            jdbcTemplate.update("""
                    INSERT INTO fraud_account_locks (account_id)
                    VALUES (?)
                    ON CONFLICT (account_id) DO NOTHING
                    """, accountId);
        }
        jdbcTemplate.queryForObject(
                """
                SELECT account_id
                FROM fraud_account_locks
                WHERE account_id = ?
                FOR UPDATE
                """,
                UUID.class,
                accountId
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
