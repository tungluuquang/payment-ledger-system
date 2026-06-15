package org.vippro.reconciliation_service.service;

import org.junit.jupiter.api.Test;
import org.vippro.reconciliation_service.dto.PaymentReconciliationReport;
import org.vippro.reconciliation_service.model.AccountTransactionRecord;
import org.vippro.reconciliation_service.model.LedgerPostingRecord;
import org.vippro.reconciliation_service.repository.AccountTransactionSource;
import org.vippro.reconciliation_service.repository.LedgerPostingSource;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReconciliationServiceTest {

    private final AccountTransactionSource accountSource =
            mock(AccountTransactionSource.class);
    private final LedgerPostingSource ledgerSource =
            mock(LedgerPostingSource.class);
    private final Clock clock = Clock.fixed(
            Instant.parse("2026-06-15T04:00:00Z"),
            ZoneOffset.UTC
    );
    private final ReconciliationService service =
            new ReconciliationService(accountSource, ledgerSource, clock);

    @Test
    void matchesPaymentAndReversalRecords() {
        UUID paymentId = UUID.randomUUID();
        UUID debitAccountId = UUID.randomUUID();
        UUID creditAccountId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();

        when(accountSource.findByPaymentId(paymentId)).thenReturn(List.of(
                accountRecord(paymentId, debitAccountId, "DEBIT", correlationId),
                accountRecord(paymentId, creditAccountId, "CREDIT", correlationId),
                accountRecord(paymentId, creditAccountId, "CREDIT_REVERSAL", correlationId),
                accountRecord(paymentId, debitAccountId, "DEBIT_REVERSAL", correlationId)
        ));
        when(ledgerSource.findByPaymentId(paymentId)).thenReturn(List.of(
                posting(paymentId, debitAccountId, "DEBIT", "PAYMENT", correlationId),
                posting(paymentId, creditAccountId, "CREDIT", "PAYMENT", correlationId),
                posting(paymentId, creditAccountId, "DEBIT", "REVERSAL", correlationId),
                posting(paymentId, debitAccountId, "CREDIT", "REVERSAL", correlationId)
        ));

        PaymentReconciliationReport report =
                service.reconcilePayment(paymentId);

        assertEquals("MATCHED", report.status());
        assertEquals(4, report.accountTransactionCount());
        assertEquals(4, report.ledgerPostingCount());
        assertEquals(List.of(), report.discrepancies());
        assertEquals(Instant.now(clock), report.checkedAt());
    }

    @Test
    void reportsRecordsMissingFromEachSource() {
        UUID paymentId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();
        when(accountSource.findByPaymentId(paymentId)).thenReturn(List.of(
                accountRecord(
                        paymentId,
                        UUID.randomUUID(),
                        "DEBIT",
                        correlationId
                )
        ));
        when(ledgerSource.findByPaymentId(paymentId)).thenReturn(List.of(
                posting(
                        paymentId,
                        UUID.randomUUID(),
                        "CREDIT",
                        "PAYMENT",
                        correlationId
                )
        ));

        PaymentReconciliationReport report =
                service.reconcilePayment(paymentId);

        assertEquals("MISMATCHED", report.status());
        assertEquals(2, report.discrepancies().size());
        assertEquals(
                "NET_EFFECT_MISMATCH",
                report.discrepancies().get(0).code()
        );
        assertEquals(
                "NET_EFFECT_MISMATCH",
                report.discrepancies().get(1).code()
        );
    }

    @Test
    void detectsDuplicateAccountTransaction() {
        UUID paymentId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();
        AccountTransactionRecord transaction = accountRecord(
                paymentId,
                accountId,
                "DEBIT",
                correlationId
        );
        when(accountSource.findByPaymentId(paymentId))
                .thenReturn(List.of(transaction, transaction));
        when(ledgerSource.findByPaymentId(paymentId)).thenReturn(List.of(
                posting(
                        paymentId,
                        accountId,
                        "DEBIT",
                        "PAYMENT",
                        correlationId
                )
        ));

        PaymentReconciliationReport report =
                service.reconcilePayment(paymentId);

        assertEquals("MISMATCHED", report.status());
        assertEquals(1, report.discrepancies().size());
        assertEquals(
                "NET_EFFECT_MISMATCH",
                report.discrepancies().getFirst().code()
        );
    }

    @Test
    void matchesCompensatedDebitWithoutLedgerActivity() {
        UUID paymentId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();
        when(accountSource.findByPaymentId(paymentId)).thenReturn(List.of(
                accountRecord(paymentId, accountId, "DEBIT", correlationId),
                accountRecord(
                        paymentId,
                        accountId,
                        "DEBIT_REVERSAL",
                        correlationId
                )
        ));
        when(ledgerSource.findByPaymentId(paymentId)).thenReturn(List.of());

        PaymentReconciliationReport report =
                service.reconcilePayment(paymentId);

        assertEquals("MATCHED", report.status());
        assertEquals(List.of(), report.discrepancies());
    }

    @Test
    void reportsNoActivityInsteadOfClaimingAnUnknownPaymentMatches() {
        UUID paymentId = UUID.randomUUID();
        when(accountSource.findByPaymentId(paymentId)).thenReturn(List.of());
        when(ledgerSource.findByPaymentId(paymentId)).thenReturn(List.of());

        PaymentReconciliationReport report =
                service.reconcilePayment(paymentId);

        assertEquals("NO_ACTIVITY", report.status());
    }

    private AccountTransactionRecord accountRecord(
            UUID paymentId,
            UUID accountId,
            String type,
            UUID correlationId
    ) {
        return new AccountTransactionRecord(
                UUID.randomUUID(),
                paymentId,
                accountId,
                type,
                new BigDecimal("25.0000"),
                "USD",
                correlationId
        );
    }

    private LedgerPostingRecord posting(
            UUID paymentId,
            UUID accountId,
            String side,
            String entryType,
            UUID correlationId
    ) {
        return new LedgerPostingRecord(
                UUID.randomUUID(),
                UUID.randomUUID(),
                paymentId,
                accountId,
                side,
                entryType,
                new BigDecimal("25.00"),
                "USD",
                correlationId
        );
    }
}
