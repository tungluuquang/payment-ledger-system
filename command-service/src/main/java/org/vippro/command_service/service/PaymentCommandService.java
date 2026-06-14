package org.vippro.command_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vippro.command.CancelPaymentCommand;
import org.vippro.command.CompletePaymentCommand;
import org.vippro.command.InitiatePaymentCommand;
import org.vippro.command_service.model.EventRecord;
import org.vippro.command_service.model.OutboxRecord;
import org.vippro.command_service.model.PaymentRecord;
import org.vippro.command_service.repository.EventRecordRepository;
import org.vippro.command_service.repository.OutboxRecordRepository;
import org.vippro.command_service.repository.PaymentRecordRepository;
import org.vippro.command_service.repository.ProcessedCommandRepository;
import org.vippro.command_service.util.OutboxStatus;
import org.vippro.command_service.util.PaymentStatus;
import org.vippro.event.PaymentCancelled;
import org.vippro.event.PaymentCompleted;
import org.vippro.event.PaymentInitiated;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentCommandService {

    private static final String PAYMENT_EVENTS_TOPIC = "payment-events";
    private static final String AGGREGATE_TYPE = "Payment";
    private static final String SERVICE_NAME = "command-service";
    private static final String EVENT_VERSION = "1";

    private final PaymentRecordRepository paymentRepository;
    private final EventRecordRepository eventRepository;
    private final OutboxRecordRepository outboxRepository;
    private final ProcessedCommandRepository processedCommandRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public UUID initiate(UUID commandId, InitiatePaymentCommand command) {
        validateCommandId(commandId);
        validateInitiate(command);

        if (!tryProcess(commandId, InitiatePaymentCommand.class)) {
            return paymentRepository.findByIdempotencyKey(
                    command.getIdempotencyKey()
            ).map(PaymentRecord::getPaymentId).orElse(null);
        }

        PaymentRecord existing = paymentRepository.findByIdempotencyKey(
                command.getIdempotencyKey()
        ).orElse(null);
        if (existing != null) {
            validateSameInitiation(existing, command);
            return existing.getPaymentId();
        }

        Instant now = Instant.now();
        UUID paymentId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        PaymentRecord payment = PaymentRecord.builder()
                .paymentId(paymentId)
                .sourceAccountId(command.getSourceAccountId())
                .destinationAccountId(command.getDestinationAccountId())
                .amount(command.getAmount())
                .currency(command.getCurrency())
                .correlationId(command.getCorrelationId())
                .idempotencyKey(command.getIdempotencyKey())
                .status(PaymentStatus.INITIATED)
                .eventVersion(1)
                .createdAt(now)
                .updatedAt(now)
                .build();

        PaymentInitiated event = PaymentInitiated.builder()
                .eventId(eventId)
                .paymentId(paymentId)
                .sourceAccountId(command.getSourceAccountId())
                .destinationAccountId(command.getDestinationAccountId())
                .amount(command.getAmount())
                .currency(command.getCurrency())
                .correlationId(command.getCorrelationId())
                .idempotencyKey(command.getIdempotencyKey())
                .occurredAt(now)
                .build();

        paymentRepository.save(payment);
        saveEvent(payment, eventId, event, now);
        return paymentId;
    }

    @Transactional
    public void complete(UUID commandId, CompletePaymentCommand command) {
        validateCommandId(commandId);
        requireNonNull(command, "command");
        requireNonNull(command.getPaymentId(), "paymentId");
        requireNonNull(command.getCorrelationId(), "correlationId");

        if (!tryProcess(commandId, CompletePaymentCommand.class)) {
            return;
        }

        PaymentRecord payment = findForUpdate(command.getPaymentId());
        validateCorrelation(payment, command.getCorrelationId());
        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            return;
        }
        if (payment.getStatus() == PaymentStatus.CANCELLED) {
            throw new IllegalStateException("Cancelled payment cannot be completed");
        }

        Instant occurredAt = command.getCompletedAt() == null
                ? Instant.now()
                : command.getCompletedAt();
        UUID eventId = UUID.randomUUID();
        PaymentCompleted event = PaymentCompleted.builder()
                .eventId(eventId)
                .paymentId(payment.getPaymentId())
                .correlationId(payment.getCorrelationId())
                .occurredAt(occurredAt)
                .build();

        transition(payment, PaymentStatus.COMPLETED, occurredAt);
        saveEvent(payment, eventId, event, occurredAt);
    }

    @Transactional
    public void cancel(UUID commandId, CancelPaymentCommand command) {
        validateCommandId(commandId);
        requireNonNull(command, "command");
        requireNonNull(command.getPaymentId(), "paymentId");
        requireNonNull(command.getCorrelationId(), "correlationId");
        if (command.getReason() == null || command.getReason().isBlank()) {
            throw new IllegalArgumentException("reason is required");
        }

        if (!tryProcess(commandId, CancelPaymentCommand.class)) {
            return;
        }

        PaymentRecord payment = findForUpdate(command.getPaymentId());
        validateCorrelation(payment, command.getCorrelationId());
        if (payment.getStatus() == PaymentStatus.CANCELLED) {
            return;
        }
        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            throw new IllegalStateException("Completed payment cannot be cancelled");
        }

        Instant occurredAt = command.getCancelledAt() == null
                ? Instant.now()
                : command.getCancelledAt();
        UUID eventId = UUID.randomUUID();
        PaymentCancelled event = PaymentCancelled.builder()
                .eventId(eventId)
                .paymentId(payment.getPaymentId())
                .correlationId(payment.getCorrelationId())
                .reason(command.getReason())
                .occurredAt(occurredAt)
                .build();

        transition(payment, PaymentStatus.CANCELLED, occurredAt);
        saveEvent(payment, eventId, event, occurredAt);
    }

    private boolean tryProcess(UUID commandId, Class<?> commandType) {
        return processedCommandRepository.insertIfAbsent(
                commandId,
                commandType.getSimpleName(),
                Instant.now()
        ) == 1;
    }

    private PaymentRecord findForUpdate(UUID paymentId) {
        return paymentRepository.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Payment not found: " + paymentId
                ));
    }

    private void transition(
            PaymentRecord payment,
            PaymentStatus status,
            Instant occurredAt
    ) {
        payment.setStatus(status);
        payment.setEventVersion(payment.getEventVersion() + 1);
        payment.setUpdatedAt(occurredAt);
    }

    private void saveEvent(
            PaymentRecord payment,
            UUID eventId,
            Object event,
            Instant occurredAt
    ) {
        String eventType = event.getClass().getSimpleName();
        String payload = serialize(event);

        eventRepository.save(EventRecord.builder()
                .eventId(eventId)
                .aggregateId(payment.getPaymentId())
                .correlationId(payment.getCorrelationId())
                .aggregateType(AGGREGATE_TYPE)
                .eventType(eventType)
                .payload(payload)
                .version(payment.getEventVersion())
                .occurredAt(occurredAt)
                .serviceName(SERVICE_NAME)
                .eventVersion(EVENT_VERSION)
                .build());

        outboxRepository.save(OutboxRecord.builder()
                .id(eventId)
                .aggregateId(payment.getPaymentId())
                .correlationId(payment.getCorrelationId())
                .eventId(eventId)
                .topic(PAYMENT_EVENTS_TOPIC)
                .eventType(eventType)
                .payload(payload)
                .createdAt(occurredAt)
                .outboxStatus(OutboxStatus.NEW)
                .nextRetryAt(Instant.now())
                .build());
    }

    private String serialize(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize payment event", e);
        }
    }

    private void validateInitiate(InitiatePaymentCommand command) {
        requireNonNull(command, "command");
        requireNonNull(command.getSourceAccountId(), "sourceAccountId");
        requireNonNull(command.getDestinationAccountId(), "destinationAccountId");
        requireNonNull(command.getCorrelationId(), "correlationId");
        requireNonNull(command.getIdempotencyKey(), "idempotencyKey");
        requireNonNull(command.getCurrency(), "currency");
        requireNonNull(command.getAmount(), "amount");
        if (command.getSourceAccountId().equals(command.getDestinationAccountId())) {
            throw new IllegalArgumentException(
                    "Source and destination accounts must differ"
            );
        }
        if (command.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
    }

    private void validateSameInitiation(
            PaymentRecord existing,
            InitiatePaymentCommand command
    ) {
        if (!Objects.equals(existing.getSourceAccountId(), command.getSourceAccountId())
                || !Objects.equals(existing.getDestinationAccountId(), command.getDestinationAccountId())
                || existing.getAmount().compareTo(command.getAmount()) != 0
                || existing.getCurrency() != command.getCurrency()
                || !Objects.equals(existing.getCorrelationId(), command.getCorrelationId())) {
            throw new IllegalStateException(
                    "Idempotency key was already used for another payment"
            );
        }
    }

    private void validateCorrelation(
            PaymentRecord payment,
            UUID correlationId
    ) {
        if (!payment.getCorrelationId().equals(correlationId)) {
            throw new IllegalArgumentException(
                    "Command correlationId does not match payment"
            );
        }
    }

    private void validateCommandId(UUID commandId) {
        requireNonNull(commandId, "commandId");
    }

    private void requireNonNull(Object value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
