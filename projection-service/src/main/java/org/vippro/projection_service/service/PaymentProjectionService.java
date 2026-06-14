package org.vippro.projection_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vippro.event.*;
import org.vippro.projection_service.model.PaymentProjection;
import org.vippro.projection_service.model.PaymentViewStatus;
import org.vippro.projection_service.model.StepViewStatus;
import org.vippro.projection_service.repository.PaymentLockStore;
import org.vippro.projection_service.repository.PaymentProjectionRepository;
import org.vippro.projection_service.repository.ProjectionEventStore;

import java.time.Instant;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PaymentProjectionService {

    private final PaymentProjectionRepository projectionRepository;
    private final ProjectionEventStore eventStore;
    private final PaymentLockStore paymentLockStore;
    private final EventMetadataExtractor metadataExtractor;
    private final ObjectMapper objectMapper;

    @Transactional
    public void project(Object event) {
        EventMetadata metadata = metadataExtractor.extract(event);
        validateMetadata(metadata);

        if (!eventStore.insertIfAbsent(
                metadata.eventId(),
                metadata.paymentId(),
                metadata.correlationId(),
                event.getClass().getSimpleName(),
                serialize(event),
                metadata.occurredAt(),
                Instant.now()
        )) {
            return;
        }

        paymentLockStore.lock(metadata.paymentId());
        PaymentProjection projection = projectionRepository
                .findById(metadata.paymentId())
                .orElseGet(() -> newProjection(metadata));
        validateCorrelation(projection, metadata.correlationId());

        apply(projection, event);
        updateLastEvent(projection, event, metadata.occurredAt());
        projectionRepository.save(projection);
    }

    private void apply(PaymentProjection projection, Object event) {
        if (event instanceof PaymentInitiated value) {
            setOrValidateCoreData(
                    projection,
                    value.getSourceAccountId(),
                    value.getDestinationAccountId(),
                    value.getAmount(),
                    value.getCurrency()
            );
            projection.setInitiatedAt(earliest(
                    projection.getInitiatedAt(),
                    value.getOccurredAt()
            ));
            if (projection.getPaymentStatus() == PaymentViewStatus.UNKNOWN) {
                projection.setPaymentStatus(PaymentViewStatus.INITIATED);
            }
            if (projection.getFraudStatus() == StepViewStatus.NOT_STARTED) {
                projection.setFraudStatus(StepViewStatus.PENDING);
            }
            return;
        }
        if (event instanceof FraudCheckPassed) {
            projection.setFraudStatus(StepViewStatus.COMPLETED);
            if (projection.getDebitStatus() == StepViewStatus.NOT_STARTED) {
                projection.setDebitStatus(StepViewStatus.PENDING);
            }
            markProcessing(projection);
            return;
        }
        if (event instanceof FraudCheckFailed value) {
            if (projection.getFraudStatus() != StepViewStatus.COMPLETED) {
                projection.setFraudStatus(StepViewStatus.FAILED);
                projection.setLastError(value.getReason());
                markFailed(projection);
            }
            return;
        }
        if (event instanceof AccountDebited value) {
            setSourceAccount(projection, value.getAccountId());
            setAmountAndCurrency(
                    projection,
                    value.getAmount(),
                    value.getCurrency()
            );
            projection.setDebitStatus(StepViewStatus.COMPLETED);
            projection.setDebitTransactionId(value.getTransactionId());
            if (projection.getLedgerStatus() == StepViewStatus.NOT_STARTED) {
                projection.setLedgerStatus(StepViewStatus.PENDING);
            }
            markProcessing(projection);
            return;
        }
        if (event instanceof AccountDebitFailed value) {
            setSourceAccount(projection, value.getAccountId());
            setAmountAndCurrency(
                    projection,
                    value.getAmount(),
                    value.getCurrency()
            );
            if (projection.getDebitStatus() != StepViewStatus.COMPLETED) {
                projection.setDebitStatus(StepViewStatus.FAILED);
                projection.setLastError(value.getReason());
                markFailed(projection);
            }
            return;
        }
        if (event instanceof AccountDebitReversed value) {
            setSourceAccount(projection, value.getAccountId());
            setAmountAndCurrency(
                    projection,
                    value.getAmount(),
                    value.getCurrency()
            );
            projection.setReversalStatus(StepViewStatus.COMPLETED);
            projection.setReversalTransactionId(
                    value.getReversalTransactionId()
            );
            if (!isTerminal(projection)) {
                projection.setPaymentStatus(PaymentViewStatus.COMPENSATING);
            }
            return;
        }
        if (event instanceof JournalEntryRecorded value) {
            setOrValidateCoreData(
                    projection,
                    value.getDebitAccountId(),
                    value.getCreditAccountId(),
                    value.getAmount(),
                    value.getCurrency()
            );
            projection.setLedgerStatus(StepViewStatus.COMPLETED);
            projection.setJournalEntryId(value.getJournalEntryId());
            markProcessing(projection);
            return;
        }
        if (event instanceof JournalEntryFailed value) {
            setSourceAccount(projection, value.getDebitAccountId());
            setDestinationAccount(
                    projection,
                    value.getCreditAccountId()
            );
            if (projection.getLedgerStatus() != StepViewStatus.COMPLETED) {
                projection.setLedgerStatus(StepViewStatus.FAILED);
                if (projection.getDebitStatus() == StepViewStatus.COMPLETED
                        && projection.getReversalStatus()
                        == StepViewStatus.NOT_STARTED) {
                    projection.setReversalStatus(StepViewStatus.PENDING);
                }
                projection.setLastError(value.getReason());
                markFailed(projection);
            }
            return;
        }
        if (event instanceof JournalEntryReversalRecorded value) {
            projection.setReversalJournalEntryId(
                    value.getReversalJournalEntryId()
            );
            projection.setReversalStatus(StepViewStatus.COMPLETED);
            setAmountAndCurrency(
                    projection,
                    value.getAmount(),
                    value.getCurrency()
            );
            if (!isTerminal(projection)) {
                projection.setPaymentStatus(PaymentViewStatus.COMPENSATING);
            }
            return;
        }
        if (event instanceof PaymentCompleted value) {
            if (projection.getCancelledAt() == null
                    || !projection.getCancelledAt()
                    .isAfter(value.getOccurredAt())) {
                projection.setPaymentStatus(PaymentViewStatus.COMPLETED);
                projection.setCompletedAt(value.getOccurredAt());
                projection.setLastError(null);
            }
            return;
        }
        if (event instanceof PaymentCancelled value) {
            if (projection.getCompletedAt() == null
                    || !projection.getCompletedAt()
                    .isAfter(value.getOccurredAt())) {
                projection.setPaymentStatus(PaymentViewStatus.CANCELLED);
                projection.setCancelledAt(value.getOccurredAt());
                projection.setLastError(value.getReason());
            }
            return;
        }
        if (event instanceof PaymentFailed value) {
            projection.setLastError(value.getReason());
            markFailed(projection);
            return;
        }
        throw new IllegalArgumentException(
                "Unsupported projection event type: "
                        + event.getClass().getName()
        );
    }

    private PaymentProjection newProjection(EventMetadata metadata) {
        Instant now = Instant.now();
        return PaymentProjection.builder()
                .paymentId(metadata.paymentId())
                .correlationId(metadata.correlationId())
                .paymentStatus(PaymentViewStatus.UNKNOWN)
                .fraudStatus(StepViewStatus.NOT_STARTED)
                .debitStatus(StepViewStatus.NOT_STARTED)
                .ledgerStatus(StepViewStatus.NOT_STARTED)
                .reversalStatus(StepViewStatus.NOT_STARTED)
                .lastEventType("UNKNOWN")
                .lastEventAt(metadata.occurredAt())
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private void updateLastEvent(
            PaymentProjection projection,
            Object event,
            Instant occurredAt
    ) {
        if (!occurredAt.isBefore(projection.getLastEventAt())) {
            projection.setLastEventAt(occurredAt);
            projection.setLastEventType(
                    event.getClass().getSimpleName()
            );
        }
        projection.setUpdatedAt(Instant.now());
    }

    private void markProcessing(PaymentProjection projection) {
        if (!isTerminal(projection)) {
            projection.setPaymentStatus(PaymentViewStatus.PROCESSING);
        }
    }

    private void markFailed(PaymentProjection projection) {
        if (!isTerminal(projection)) {
            projection.setPaymentStatus(PaymentViewStatus.FAILED);
        }
    }

    private boolean isTerminal(PaymentProjection projection) {
        return projection.getPaymentStatus() == PaymentViewStatus.COMPLETED
                || projection.getPaymentStatus()
                == PaymentViewStatus.CANCELLED;
    }

    private void setOrValidateCoreData(
            PaymentProjection projection,
            java.util.UUID sourceAccountId,
            java.util.UUID destinationAccountId,
            java.math.BigDecimal amount,
            org.vippro.util.CurrencyType currency
    ) {
        setSourceAccount(projection, sourceAccountId);
        setDestinationAccount(projection, destinationAccountId);
        setAmountAndCurrency(projection, amount, currency);
    }

    private void setSourceAccount(
            PaymentProjection projection,
            java.util.UUID accountId
    ) {
        if (projection.getSourceAccountId() == null) {
            projection.setSourceAccountId(accountId);
        } else if (accountId != null
                && !projection.getSourceAccountId().equals(accountId)) {
            throw new IllegalStateException(
                    "Event source account does not match projection"
            );
        }
    }

    private void setDestinationAccount(
            PaymentProjection projection,
            java.util.UUID accountId
    ) {
        if (projection.getDestinationAccountId() == null) {
            projection.setDestinationAccountId(accountId);
        } else if (accountId != null
                && !projection.getDestinationAccountId().equals(accountId)) {
            throw new IllegalStateException(
                    "Event destination account does not match projection"
            );
        }
    }

    private void setAmountAndCurrency(
            PaymentProjection projection,
            java.math.BigDecimal amount,
            org.vippro.util.CurrencyType currency
    ) {
        if (projection.getAmount() == null) {
            projection.setAmount(amount);
        } else if (amount != null
                && projection.getAmount().compareTo(amount) != 0) {
            throw new IllegalStateException(
                    "Event amount does not match projection"
            );
        }
        if (projection.getCurrency() == null) {
            projection.setCurrency(currency);
        } else if (currency != null
                && projection.getCurrency() != currency) {
            throw new IllegalStateException(
                    "Event currency does not match projection"
            );
        }
    }

    private void validateCorrelation(
            PaymentProjection projection,
            java.util.UUID correlationId
    ) {
        if (projection.getCorrelationId() == null) {
            projection.setCorrelationId(correlationId);
        } else if (correlationId != null
                && !projection.getCorrelationId().equals(correlationId)) {
            throw new IllegalStateException(
                    "Event correlationId does not match projection"
            );
        }
    }

    private void validateMetadata(EventMetadata metadata) {
        if (metadata.eventId() == null
                || metadata.paymentId() == null
                || metadata.occurredAt() == null) {
            throw new IllegalArgumentException(
                    "Projection event metadata is incomplete"
            );
        }
    }

    private String serialize(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Could not serialize projection event",
                    e
            );
        }
    }

    private Instant earliest(Instant current, Instant candidate) {
        if (current == null || candidate.isBefore(current)) {
            return candidate;
        }
        return current;
    }
}
