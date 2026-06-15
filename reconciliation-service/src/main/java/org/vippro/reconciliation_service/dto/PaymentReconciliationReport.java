package org.vippro.reconciliation_service.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PaymentReconciliationReport(
        UUID paymentId,
        String status,
        int accountTransactionCount,
        int ledgerPostingCount,
        List<Discrepancy> discrepancies,
        Instant checkedAt
) {
}
