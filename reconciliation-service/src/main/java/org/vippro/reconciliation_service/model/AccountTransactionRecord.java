package org.vippro.reconciliation_service.model;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountTransactionRecord(
        UUID transactionId,
        UUID paymentId,
        UUID accountId,
        String transactionType,
        BigDecimal amount,
        String currency,
        UUID correlationId
) {
}
