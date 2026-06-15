package org.vippro.reconciliation_service.repository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.vippro.reconciliation_service.model.LedgerPostingRecord;

import java.util.List;
import java.util.UUID;

@Repository
public class LedgerPostingSource {

    private final JdbcClient jdbcClient;

    public LedgerPostingSource(
            @Qualifier("ledgerJdbcClient") JdbcClient jdbcClient
    ) {
        this.jdbcClient = jdbcClient;
    }

    public List<LedgerPostingRecord> findByPaymentId(UUID paymentId) {
        return jdbcClient.sql("""
                        SELECT p.posting_id, p.journal_entry_id,
                               j.payment_id, p.account_id, p.side,
                               j.entry_type, p.amount, p.currency,
                               j.correlation_id
                        FROM ledger_postings p
                        JOIN journal_entries j
                          ON j.journal_entry_id = p.journal_entry_id
                        WHERE j.payment_id = :paymentId
                        ORDER BY j.created_at, p.posting_id
                        """)
                .param("paymentId", paymentId)
                .query((resultSet, rowNumber) ->
                        new LedgerPostingRecord(
                                resultSet.getObject("posting_id", UUID.class),
                                resultSet.getObject(
                                        "journal_entry_id",
                                        UUID.class
                                ),
                                resultSet.getObject("payment_id", UUID.class),
                                resultSet.getObject("account_id", UUID.class),
                                resultSet.getString("side"),
                                resultSet.getString("entry_type"),
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
