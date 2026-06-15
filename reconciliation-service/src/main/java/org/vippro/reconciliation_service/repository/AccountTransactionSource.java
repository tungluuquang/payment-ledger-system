package org.vippro.reconciliation_service.repository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.vippro.reconciliation_service.model.AccountTransactionRecord;

import java.util.List;
import java.util.UUID;

@Repository
public class AccountTransactionSource {

    private final JdbcClient jdbcClient;

    public AccountTransactionSource(
            @Qualifier("accountJdbcClient") JdbcClient jdbcClient
    ) {
        this.jdbcClient = jdbcClient;
    }

    public List<AccountTransactionRecord> findByPaymentId(UUID paymentId) {
        return jdbcClient.sql("""
                        SELECT transaction_id, payment_id, account_id, type,
                               amount, currency, correlation_id
                        FROM account_transactions
                        WHERE payment_id = :paymentId
                        ORDER BY created_at, transaction_id
                        """)
                .param("paymentId", paymentId)
                .query((resultSet, rowNumber) ->
                        new AccountTransactionRecord(
                                resultSet.getObject(
                                        "transaction_id",
                                        UUID.class
                                ),
                                resultSet.getObject("payment_id", UUID.class),
                                resultSet.getObject("account_id", UUID.class),
                                resultSet.getString("type"),
                                resultSet.getBigDecimal("amount"),
                                resultSet.getString("currency"),
                                resultSet.getObject(
                                        "correlation_id",
                                        UUID.class
                                )
                        ))
                .list();
    }
}
