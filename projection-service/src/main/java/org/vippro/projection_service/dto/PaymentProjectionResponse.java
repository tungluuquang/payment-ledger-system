package org.vippro.projection_service.dto;

import org.vippro.projection_service.model.PaymentProjection;
import org.vippro.projection_service.model.PaymentViewStatus;
import org.vippro.projection_service.model.StepViewStatus;
import org.vippro.util.CurrencyType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentProjectionResponse(
        UUID paymentId,
        UUID correlationId,
        UUID sourceAccountId,
        UUID destinationAccountId,
        BigDecimal amount,
        CurrencyType currency,
        PaymentViewStatus paymentStatus,
        StepViewStatus fraudStatus,
        StepViewStatus debitStatus,
        StepViewStatus ledgerStatus,
        StepViewStatus reversalStatus,
        UUID debitTransactionId,
        UUID reversalTransactionId,
        UUID journalEntryId,
        UUID reversalJournalEntryId,
        String lastEventType,
        String lastError,
        Instant initiatedAt,
        Instant completedAt,
        Instant cancelledAt,
        Instant lastEventAt,
        Instant updatedAt
) {
    public static PaymentProjectionResponse from(
            PaymentProjection projection
    ) {
        return new PaymentProjectionResponse(
                projection.getPaymentId(),
                projection.getCorrelationId(),
                projection.getSourceAccountId(),
                projection.getDestinationAccountId(),
                projection.getAmount(),
                projection.getCurrency(),
                projection.getPaymentStatus(),
                projection.getFraudStatus(),
                projection.getDebitStatus(),
                projection.getLedgerStatus(),
                projection.getReversalStatus(),
                projection.getDebitTransactionId(),
                projection.getReversalTransactionId(),
                projection.getJournalEntryId(),
                projection.getReversalJournalEntryId(),
                projection.getLastEventType(),
                projection.getLastError(),
                projection.getInitiatedAt(),
                projection.getCompletedAt(),
                projection.getCancelledAt(),
                projection.getLastEventAt(),
                projection.getUpdatedAt()
        );
    }
}
