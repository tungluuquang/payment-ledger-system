package org.vippro.audit_service.service;

import org.springframework.stereotype.Component;
import org.vippro.event.*;

@Component
public class AuditEventMetadataExtractor {

    AuditEventMetadata extract(Object event) {
        if (event instanceof PaymentInitiated value) {
            return metadata(value.getEventId(), value.getPaymentId(),
                    value.getCorrelationId(), value.getOccurredAt());
        }
        if (event instanceof PaymentCompleted value) {
            return metadata(value.getEventId(), value.getPaymentId(),
                    value.getCorrelationId(), value.getOccurredAt());
        }
        if (event instanceof PaymentCancelled value) {
            return metadata(value.getEventId(), value.getPaymentId(),
                    value.getCorrelationId(), value.getOccurredAt());
        }
        if (event instanceof PaymentFailed value) {
            return metadata(value.getEventId(), value.getPaymentId(),
                    value.getCorrelationId(), value.getOccurredAt());
        }
        if (event instanceof FraudCheckPassed value) {
            return metadata(value.getEventId(), value.getPaymentId(),
                    value.getCorrelationId(), value.getOccurredAt());
        }
        if (event instanceof FraudCheckFailed value) {
            return metadata(value.getEventId(), value.getPaymentId(),
                    value.getCorrelationId(), value.getOccurredAt());
        }
        if (event instanceof AccountDebited value) {
            return metadata(value.getEventId(), value.getPaymentId(),
                    value.getCorrelationId(), value.getOccurredAt());
        }
        if (event instanceof AccountDebitFailed value) {
            return metadata(value.getEventId(), value.getPaymentId(),
                    value.getCorrelationId(), value.getOccurredAt());
        }
        if (event instanceof AccountDebitReversed value) {
            return metadata(value.getEventId(), value.getPaymentId(),
                    value.getCorrelationId(), value.getOccurredAt());
        }
        if (event instanceof AccountCredited value) {
            return metadata(value.getEventId(), value.getPaymentId(),
                    value.getCorrelationId(), value.getOccurredAt());
        }
        if (event instanceof AccountCreditFailed value) {
            return metadata(value.getEventId(), value.getPaymentId(),
                    value.getCorrelationId(), value.getOccurredAt());
        }
        if (event instanceof AccountCreditReversed value) {
            return metadata(value.getEventId(), value.getPaymentId(),
                    value.getCorrelationId(), value.getOccurredAt());
        }
        if (event instanceof JournalEntryRecorded value) {
            return metadata(value.getEventId(), value.getPaymentId(),
                    value.getCorrelationId(), value.getOccurredAt());
        }
        if (event instanceof JournalEntryFailed value) {
            return metadata(value.getEventId(), value.getPaymentId(),
                    value.getCorrelationId(), value.getOccurredAt());
        }
        if (event instanceof JournalEntryReversalRecorded value) {
            return metadata(value.getEventId(), value.getPaymentId(),
                    value.getCorrelationId(), value.getOccurredAt());
        }
        throw new IllegalArgumentException(
                "Unsupported audit event type: "
                        + event.getClass().getName()
        );
    }

    private AuditEventMetadata metadata(
            java.util.UUID eventId,
            java.util.UUID paymentId,
            java.util.UUID correlationId,
            java.time.Instant occurredAt
    ) {
        if (eventId == null || paymentId == null || occurredAt == null) {
            throw new IllegalArgumentException(
                    "Audit event metadata is incomplete"
            );
        }
        return new AuditEventMetadata(
                eventId,
                paymentId,
                correlationId,
                occurredAt
        );
    }
}
