package org.vippro.reconciliation_service.model;

import java.math.BigDecimal;
import java.util.UUID;

public record LedgerPostingRecord(
        UUID postingId,
        UUID journalEntryId,
        UUID paymentId,
        UUID accountId,
        String postingSide,
        String journalEntryType,
        BigDecimal amount,
        String currency,
        UUID correlationId
) {
}
