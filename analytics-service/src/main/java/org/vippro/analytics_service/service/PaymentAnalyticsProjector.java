package org.vippro.analytics_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vippro.analytics_service.model.AnalyticsPaymentStatus;
import org.vippro.analytics_service.model.PaymentAnalytics;
import org.vippro.analytics_service.repository.AnalyticsEventStore;
import org.vippro.analytics_service.repository.PaymentAnalyticsRepository;
import org.vippro.event.*;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class PaymentAnalyticsProjector {

    private final AnalyticsEventStore eventStore;
    private final PaymentAnalyticsRepository paymentRepository;
    private final AnalyticsEventMetadataExtractor metadataExtractor;

    @Transactional
    public void project(Object event) {
        AnalyticsEventMetadata metadata = metadataExtractor.extract(event);
        Instant processedAt = Instant.now();
        if (!eventStore.insertIfAbsent(
                metadata,
                event.getClass().getSimpleName(),
                processedAt
        )) {
            return;
        }

        PaymentAnalytics payment = paymentRepository
                .findById(metadata.paymentId())
                .orElseGet(() -> newPayment(metadata));
        validateCorrelation(payment, metadata);
        apply(payment, event, metadata);
        if (!metadata.occurredAt().isBefore(payment.getLastEventAt())) {
            payment.setLastEventAt(metadata.occurredAt());
            payment.setLastEventType(event.getClass().getSimpleName());
        }
        payment.setUpdatedAt(Instant.now());
        paymentRepository.save(payment);
    }

    private PaymentAnalytics newPayment(AnalyticsEventMetadata metadata) {
        return PaymentAnalytics.builder()
                .paymentId(metadata.paymentId())
                .correlationId(metadata.correlationId())
                .status(AnalyticsPaymentStatus.PROCESSING)
                .lastEventType("UNKNOWN")
                .lastEventAt(metadata.occurredAt())
                .updatedAt(Instant.now())
                .build();
    }

    private void apply(
            PaymentAnalytics payment,
            Object event,
            AnalyticsEventMetadata metadata
    ) {
        if (event instanceof PaymentInitiated value) {
            setCoreData(payment, value);
            payment.setInitiatedAt(earliest(
                    payment.getInitiatedAt(),
                    value.getOccurredAt()
            ));
            if (!isTerminal(payment)) {
                payment.setStatus(AnalyticsPaymentStatus.INITIATED);
            }
            return;
        }
        if (event instanceof PaymentCompleted value) {
            if (isLatestTerminal(payment, value.getOccurredAt())) {
                payment.setStatus(AnalyticsPaymentStatus.COMPLETED);
                payment.setCompletedAt(value.getOccurredAt());
                clearFailure(payment);
            }
            return;
        }
        if (event instanceof PaymentFailed value) {
            if (isLatestTerminal(payment, value.getOccurredAt())) {
                payment.setStatus(AnalyticsPaymentStatus.FAILED);
                payment.setFailedAt(value.getOccurredAt());
                setFailure(payment, metadata);
            }
            return;
        }
        if (event instanceof PaymentCancelled value) {
            if (isLatestTerminal(payment, value.getOccurredAt())) {
                payment.setStatus(AnalyticsPaymentStatus.CANCELLED);
                payment.setCancelledAt(value.getOccurredAt());
                if (payment.getFailureStage() == null) {
                    setFailure(payment, metadata);
                } else if (payment.getFailureReason() == null) {
                    payment.setFailureReason(value.getReason());
                }
            }
            return;
        }
        if (metadata.failureStage() != null && !isTerminal(payment)) {
            setFailure(payment, metadata);
            payment.setStatus(AnalyticsPaymentStatus.PROCESSING);
            return;
        }
        if (!isTerminal(payment)) {
            payment.setStatus(AnalyticsPaymentStatus.PROCESSING);
        }
    }

    private void setCoreData(
            PaymentAnalytics payment,
            PaymentInitiated event
    ) {
        payment.setSourceAccountId(event.getSourceAccountId());
        payment.setDestinationAccountId(event.getDestinationAccountId());
        payment.setAmount(event.getAmount());
        payment.setCurrency(
                event.getCurrency() == null
                        ? null
                        : event.getCurrency().name()
        );
    }

    private void validateCorrelation(
            PaymentAnalytics payment,
            AnalyticsEventMetadata metadata
    ) {
        if (payment.getCorrelationId() == null) {
            payment.setCorrelationId(metadata.correlationId());
        } else if (metadata.correlationId() != null
                && !payment.getCorrelationId()
                .equals(metadata.correlationId())) {
            throw new IllegalStateException(
                    "Analytics event correlation does not match payment"
            );
        }
    }

    private void setFailure(
            PaymentAnalytics payment,
            AnalyticsEventMetadata metadata
    ) {
        payment.setFailureStage(metadata.failureStage());
        payment.setFailureCode(metadata.errorCode());
        payment.setFailureReason(metadata.reason());
    }

    private void clearFailure(PaymentAnalytics payment) {
        payment.setFailureStage(null);
        payment.setFailureCode(null);
        payment.setFailureReason(null);
    }

    private boolean isLatestTerminal(
            PaymentAnalytics payment,
            Instant occurredAt
    ) {
        Instant terminalAt = latest(
                payment.getCompletedAt(),
                payment.getFailedAt(),
                payment.getCancelledAt()
        );
        return terminalAt == null || !occurredAt.isBefore(terminalAt);
    }

    private boolean isTerminal(PaymentAnalytics payment) {
        return payment.getStatus() == AnalyticsPaymentStatus.COMPLETED
                || payment.getStatus() == AnalyticsPaymentStatus.FAILED
                || payment.getStatus() == AnalyticsPaymentStatus.CANCELLED;
    }

    private Instant earliest(Instant left, Instant right) {
        if (left == null) {
            return right;
        }
        return left.isBefore(right) ? left : right;
    }

    private Instant latest(Instant... values) {
        Instant result = null;
        for (Instant value : values) {
            if (value != null
                    && (result == null || value.isAfter(result))) {
                result = value;
            }
        }
        return result;
    }
}
