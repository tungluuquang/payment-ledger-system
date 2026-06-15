package org.vippro.reconciliation_service.dto;

import java.time.Instant;
import java.util.List;

public record BatchReconciliationReport(
        int checkedPayments,
        int matchedPayments,
        int mismatchedPayments,
        int noActivityPayments,
        List<PaymentReconciliationReport> reports,
        Instant checkedAt
) {
}
