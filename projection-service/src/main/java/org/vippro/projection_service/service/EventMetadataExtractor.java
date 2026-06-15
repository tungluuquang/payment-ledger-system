package org.vippro.projection_service.service;

import org.springframework.stereotype.Component;
import org.vippro.event.*;

@Component
public class EventMetadataExtractor {

    public EventMetadata extract(Object event) {
        if (event instanceof PaymentInitiated value) {
            return new EventMetadata(
                    value.getEventId(),
                    value.getPaymentId(),
                    value.getCorrelationId(),
                    value.getOccurredAt()
            );
        }
        if (event instanceof PaymentCompleted value) {
            return metadata(
                    value.getEventId(),
                    value.getPaymentId(),
                    value.getCorrelationId(),
                    value.getOccurredAt()
            );
        }
        if (event instanceof PaymentCancelled value) {
            return metadata(
                    value.getEventId(),
                    value.getPaymentId(),
                    value.getCorrelationId(),
                    value.getOccurredAt()
            );
        }
        if (event instanceof PaymentFailed value) {
            return metadata(
                    value.getEventId(),
                    value.getPaymentId(),
                    value.getCorrelationId(),
                    value.getOccurredAt()
            );
        }
        if (event instanceof FraudCheckPassed value) {
            return metadata(
                    value.getEventId(),
                    value.getPaymentId(),
                    value.getCorrelationId(),
                    value.getOccurredAt()
            );
        }
        if (event instanceof FraudCheckFailed value) {
            return metadata(
                    value.getEventId(),
                    value.getPaymentId(),
                    value.getCorrelationId(),
                    value.getOccurredAt()
            );
        }
        if (event instanceof AccountDebited value) {
            return metadata(
                    value.getEventId(),
                    value.getPaymentId(),
                    value.getCorrelationId(),
                    value.getOccurredAt()
            );
        }
        if (event instanceof AccountDebitFailed value) {
            return metadata(
                    value.getEventId(),
                    value.getPaymentId(),
                    value.getCorrelationId(),
                    value.getOccurredAt()
            );
        }
        if (event instanceof AccountDebitReversed value) {
            return metadata(
                    value.getEventId(),
                    value.getPaymentId(),
                    value.getCorrelationId(),
                    value.getOccurredAt()
            );
        }
        if (event instanceof AccountCredited value) {
            return metadata(
                    value.getEventId(),
                    value.getPaymentId(),
                    value.getCorrelationId(),
                    value.getOccurredAt()
            );
        }
        if (event instanceof AccountCreditFailed value) {
            return metadata(
                    value.getEventId(),
                    value.getPaymentId(),
                    value.getCorrelationId(),
                    value.getOccurredAt()
            );
        }
        if (event instanceof AccountCreditReversed value) {
            return metadata(
                    value.getEventId(),
                    value.getPaymentId(),
                    value.getCorrelationId(),
                    value.getOccurredAt()
            );
        }
        if (event instanceof JournalEntryRecorded value) {
            return metadata(
                    value.getEventId(),
                    value.getPaymentId(),
                    value.getCorrelationId(),
                    value.getOccurredAt()
            );
        }
        if (event instanceof JournalEntryFailed value) {
            return metadata(
                    value.getEventId(),
                    value.getPaymentId(),
                    value.getCorrelationId(),
                    value.getOccurredAt()
            );
        }
        if (event instanceof JournalEntryReversalRecorded value) {
            return metadata(
                    value.getEventId(),
                    value.getPaymentId(),
                    value.getCorrelationId(),
                    value.getOccurredAt()
            );
        }
        throw new IllegalArgumentException(
                "Unsupported projection event type: "
                        + event.getClass().getName()
        );
    }

    private EventMetadata metadata(
            java.util.UUID eventId,
            java.util.UUID paymentId,
            java.util.UUID correlationId,
            java.time.Instant occurredAt
    ) {
        return new EventMetadata(
                eventId,
                paymentId,
                correlationId,
                occurredAt
        );
    }
}
