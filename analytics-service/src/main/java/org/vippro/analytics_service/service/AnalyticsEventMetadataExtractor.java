package org.vippro.analytics_service.service;

import org.springframework.stereotype.Component;
import org.vippro.event.*;

@Component
public class AnalyticsEventMetadataExtractor {

    AnalyticsEventMetadata extract(Object event) {
        if (event instanceof PaymentInitiated value) {
            return metadata(value.getEventId(), value.getPaymentId(),
                    value.getCorrelationId(), value.getOccurredAt(),
                    "LIFECYCLE", null, null, null);
        }
        if (event instanceof PaymentCompleted value) {
            return metadata(value.getEventId(), value.getPaymentId(),
                    value.getCorrelationId(), value.getOccurredAt(),
                    "LIFECYCLE", null, null, null);
        }
        if (event instanceof PaymentCancelled value) {
            return metadata(value.getEventId(), value.getPaymentId(),
                    value.getCorrelationId(), value.getOccurredAt(),
                    "FAILURE",
                    value.getFailedStep() == null
                            ? "PAYMENT"
                            : value.getFailedStep().name(),
                    null,
                    value.getReason());
        }
        if (event instanceof PaymentFailed value) {
            return metadata(value.getEventId(), value.getPaymentId(),
                    value.getCorrelationId(), value.getOccurredAt(),
                    "FAILURE", "PAYMENT", value.getErrorCode(),
                    value.getReason());
        }
        if (event instanceof FraudCheckPassed value) {
            return metadata(value.getEventId(), value.getPaymentId(),
                    value.getCorrelationId(), value.getOccurredAt(),
                    "FRAUD", null, null, null);
        }
        if (event instanceof FraudCheckFailed value) {
            return metadata(value.getEventId(), value.getPaymentId(),
                    value.getCorrelationId(), value.getOccurredAt(),
                    "FAILURE", "FRAUD", null, value.getReason());
        }
        if (event instanceof AccountDebited value) {
            return metadata(value.getEventId(), value.getPaymentId(),
                    value.getCorrelationId(), value.getOccurredAt(),
                    "ACCOUNT", null, null, null);
        }
        if (event instanceof AccountDebitFailed value) {
            return metadata(value.getEventId(), value.getPaymentId(),
                    value.getCorrelationId(), value.getOccurredAt(),
                    "FAILURE", "DEBIT", value.getErrorCode(),
                    value.getReason());
        }
        if (event instanceof AccountDebitReversed value) {
            return metadata(value.getEventId(), value.getPaymentId(),
                    value.getCorrelationId(), value.getOccurredAt(),
                    "REVERSAL", null, null, null);
        }
        if (event instanceof AccountCredited value) {
            return metadata(value.getEventId(), value.getPaymentId(),
                    value.getCorrelationId(), value.getOccurredAt(),
                    "ACCOUNT", null, null, null);
        }
        if (event instanceof AccountCreditFailed value) {
            return metadata(value.getEventId(), value.getPaymentId(),
                    value.getCorrelationId(), value.getOccurredAt(),
                    "FAILURE", "CREDIT", value.getErrorCode(),
                    value.getReason());
        }
        if (event instanceof AccountCreditReversed value) {
            return metadata(value.getEventId(), value.getPaymentId(),
                    value.getCorrelationId(), value.getOccurredAt(),
                    "REVERSAL", null, null, null);
        }
        if (event instanceof JournalEntryRecorded value) {
            return metadata(value.getEventId(), value.getPaymentId(),
                    value.getCorrelationId(), value.getOccurredAt(),
                    "LEDGER", null, null, null);
        }
        if (event instanceof JournalEntryFailed value) {
            return metadata(value.getEventId(), value.getPaymentId(),
                    value.getCorrelationId(), value.getOccurredAt(),
                    "FAILURE", "LEDGER", value.getErrorCode(),
                    value.getReason());
        }
        if (event instanceof JournalEntryReversalRecorded value) {
            return metadata(value.getEventId(), value.getPaymentId(),
                    value.getCorrelationId(), value.getOccurredAt(),
                    "REVERSAL", null, null, null);
        }
        throw new IllegalArgumentException(
                "Unsupported analytics event type: "
                        + event.getClass().getName()
        );
    }

    private AnalyticsEventMetadata metadata(
            java.util.UUID eventId,
            java.util.UUID paymentId,
            java.util.UUID correlationId,
            java.time.Instant occurredAt,
            String category,
            String failureStage,
            String errorCode,
            String reason
    ) {
        if (eventId == null || paymentId == null || occurredAt == null) {
            throw new IllegalArgumentException(
                    "Analytics event metadata is incomplete"
            );
        }
        return new AnalyticsEventMetadata(
                eventId,
                paymentId,
                correlationId,
                occurredAt,
                category,
                failureStage,
                errorCode,
                reason
        );
    }
}
