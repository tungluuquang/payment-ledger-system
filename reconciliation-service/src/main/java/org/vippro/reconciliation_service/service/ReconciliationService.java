package org.vippro.reconciliation_service.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.vippro.reconciliation_service.dto.BatchReconciliationReport;
import org.vippro.reconciliation_service.dto.Discrepancy;
import org.vippro.reconciliation_service.dto.PaymentReconciliationReport;
import org.vippro.reconciliation_service.model.AccountTransactionRecord;
import org.vippro.reconciliation_service.model.LedgerPostingRecord;
import org.vippro.reconciliation_service.model.ReconciliationKey;
import org.vippro.reconciliation_service.repository.AccountTransactionSource;
import org.vippro.reconciliation_service.repository.LedgerPostingSource;

import java.time.Clock;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ReconciliationService {

    private final AccountTransactionSource accountSource;
    private final LedgerPostingSource ledgerSource;
    private final Clock clock;

    @Autowired
    public ReconciliationService(
            AccountTransactionSource accountSource,
            LedgerPostingSource ledgerSource
    ) {
        this(accountSource, ledgerSource, Clock.systemUTC());
    }

    ReconciliationService(
            AccountTransactionSource accountSource,
            LedgerPostingSource ledgerSource,
            Clock clock
    ) {
        this.accountSource = accountSource;
        this.ledgerSource = ledgerSource;
        this.clock = clock;
    }

    public PaymentReconciliationReport reconcilePayment(UUID paymentId) {
        List<AccountTransactionRecord> transactions =
                accountSource.findByPaymentId(paymentId);
        List<LedgerPostingRecord> postings =
                ledgerSource.findByPaymentId(paymentId);

        Map<ReconciliationKey, BigDecimal> accountEffects =
                aggregateTransactions(transactions);
        Map<ReconciliationKey, BigDecimal> ledgerEffects =
                aggregatePostings(postings);
        List<Discrepancy> discrepancies = compare(
                accountEffects,
                ledgerEffects
        );
        String status = transactions.isEmpty() && postings.isEmpty()
                ? "NO_ACTIVITY"
                : discrepancies.isEmpty() ? "MATCHED" : "MISMATCHED";

        return new PaymentReconciliationReport(
                paymentId,
                status,
                transactions.size(),
                postings.size(),
                List.copyOf(discrepancies),
                Instant.now(clock)
        );
    }

    public BatchReconciliationReport reconcilePayments(
            List<UUID> paymentIds
    ) {
        List<PaymentReconciliationReport> reports = paymentIds.stream()
                .distinct()
                .map(this::reconcilePayment)
                .toList();
        int matched = (int) reports.stream()
                .filter(report -> report.status().equals("MATCHED"))
                .count();
        int noActivity = (int) reports.stream()
                .filter(report -> report.status().equals("NO_ACTIVITY"))
                .count();
        return new BatchReconciliationReport(
                reports.size(),
                matched,
                reports.size() - matched - noActivity,
                noActivity,
                reports,
                Instant.now(clock)
        );
    }

    private Map<ReconciliationKey, BigDecimal> aggregateTransactions(
            List<AccountTransactionRecord> transactions
    ) {
        Map<ReconciliationKey, BigDecimal> effects = new LinkedHashMap<>();
        for (AccountTransactionRecord transaction : transactions) {
            ReconciliationKey key = new ReconciliationKey(
                    transaction.accountId(),
                    transaction.currency(),
                    transaction.correlationId()
            );
            effects.merge(
                    key,
                    signedAccountAmount(transaction),
                    BigDecimal::add
            );
        }
        return effects;
    }

    private Map<ReconciliationKey, BigDecimal> aggregatePostings(
            List<LedgerPostingRecord> postings
    ) {
        Map<ReconciliationKey, BigDecimal> effects = new LinkedHashMap<>();
        for (LedgerPostingRecord posting : postings) {
            ReconciliationKey key = new ReconciliationKey(
                    posting.accountId(),
                    posting.currency(),
                    posting.correlationId()
            );
            effects.merge(
                    key,
                    signedLedgerAmount(posting),
                    BigDecimal::add
            );
        }
        return effects;
    }

    private List<Discrepancy> compare(
            Map<ReconciliationKey, BigDecimal> accountEffects,
            Map<ReconciliationKey, BigDecimal> ledgerEffects
    ) {
        List<Discrepancy> discrepancies = new ArrayList<>();
        Set<ReconciliationKey> keys = new LinkedHashSet<>();
        keys.addAll(accountEffects.keySet());
        keys.addAll(ledgerEffects.keySet());
        for (ReconciliationKey key : keys) {
            BigDecimal accountAmount = accountEffects.getOrDefault(
                    key,
                    BigDecimal.ZERO
            );
            BigDecimal ledgerAmount = ledgerEffects.getOrDefault(
                    key,
                    BigDecimal.ZERO
            );
            if (accountAmount.compareTo(ledgerAmount) != 0) {
                discrepancies.add(new Discrepancy(
                        "NET_EFFECT_MISMATCH",
                        "Account and ledger net effects do not match",
                        key,
                        accountAmount.stripTrailingZeros(),
                        ledgerAmount.stripTrailingZeros()
                ));
            }
        }
        return discrepancies;
    }

    private BigDecimal signedAccountAmount(
            AccountTransactionRecord transaction
    ) {
        return switch (transaction.transactionType()) {
            case "DEBIT", "CREDIT_REVERSAL" ->
                    transaction.amount().negate();
            case "CREDIT", "DEBIT_REVERSAL" -> transaction.amount();
            default -> throw new IllegalStateException(
                    "Unsupported account transaction type: "
                            + transaction.transactionType()
            );
        };
    }

    private BigDecimal signedLedgerAmount(
            LedgerPostingRecord posting
    ) {
        return switch (posting.postingSide()) {
            case "DEBIT" -> posting.amount().negate();
            case "CREDIT" -> posting.amount();
            default -> throw new IllegalStateException(
                    "Unsupported ledger posting side: "
                            + posting.postingSide()
            );
        };
    }
}
