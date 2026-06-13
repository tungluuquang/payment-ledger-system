package org.vippro.saga_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.vippro.command.*;
import org.vippro.event.*;
import org.vippro.saga_service.model.OutboxCommand;
import org.vippro.saga_service.model.OutboxStatus;
import org.vippro.saga_service.model.SagaState;
import org.vippro.saga_service.repository.OutboxCommandRepository;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxCommandService {
    private final OutboxCommandRepository outboxCommandRepository;
    private final ObjectMapper objectMapper;

    public void requestFraudCheck(SagaState saga, PaymentInitiated event) {
        FraudCheckRequestedCommand cmd = FraudCheckRequestedCommand.builder()
                .paymentId(event.getPaymentId())
                .accountId(event.getSourceAccountId())
                .amount(event.getAmount())
                .currency(event.getCurrency())
                .idempotencyKey(event.getIdempotencyKey())
                .correlationId(event.getCorrelationId())
                .requestedAt(Instant.now())
                .build();

        saveOutbox(
                saga.getSagaId(),
                "fraud-check-commands",
                FraudCheckRequestedCommand.class.getSimpleName(),
                cmd
        );
    }

    public void requestAccountDebit(SagaState saga, FraudCheckPassed event) {
        AccountDebitRequestedCommand cmd = AccountDebitRequestedCommand.builder()
                .paymentId(event.getPaymentId())
                .accountId(saga.getSourceAccountId())
                .amount(saga.getAmount())
                .currency(saga.getCurrency())
                .idempotencyKey(UUID.randomUUID())
                .correlationId(event.getCorrelationId())
                .requestedAt(Instant.now())
                .build();

        saveOutbox(
                saga.getSagaId(),
                "account-commands",
                AccountDebitRequestedCommand.class.getSimpleName(),
                cmd
        );
    }

    public void requestCancelPayment(SagaState saga, String reason) {
        CancelPaymentCommand cmd = CancelPaymentCommand.builder()
                .paymentId(saga.getPaymentId())
                .correlationId(saga.getCorrelationId())
                .cancelledAt(Instant.now())
                .reason(reason)
                .build();

        saveOutbox(
                saga.getSagaId(),
                "payment-commands",
                CancelPaymentCommand.class.getSimpleName(),
                cmd
        );
    }

    public void requestJournalEntry(SagaState saga, AccountDebited event) {
        JournalEntryRequestedCommand cmd =
            JournalEntryRequestedCommand.builder()
                    .paymentId(saga.getPaymentId())
                    .debitAccountId(saga.getSourceAccountId())
                    .creditAccountId(saga.getDestinationAccountId())
                    .amount(event.getAmount())
                    .currency(event.getCurrency())
                    .correlationId(saga.getCorrelationId())
                    .idempotencyKey(UUID.randomUUID())
                    .requestedAt(Instant.now())
                    .build();

        saveOutbox(
                saga.getSagaId(),
                "ledger-commands",
                JournalEntryRequestedCommand.class.getSimpleName(),
                cmd
        );
    }

    public void requestCompletePayment(UUID sagaId, PaymentCompleted event) {
        CompletePaymentCommand cmd = CompletePaymentCommand.builder()
                .paymentId(event.getPaymentId())
                .correlationId(event.getCorrelationId())
                .completedAt(Instant.now())
                .build();

        saveOutbox(
                sagaId,
                "payment-commands",
                CompletePaymentCommand.class.getSimpleName(),
                cmd
        );
    }

    private void saveOutbox(
            UUID sagaId,
            String topic,
            String commandType,
            Object payload
    ) {
        try {
            outboxCommandRepository.save(
                    OutboxCommand.builder()
                            .id(UUID.randomUUID())
                            .sagaId(sagaId)
                            .topic(topic)
                            .commandType(commandType)
                            .payload(objectMapper.writeValueAsString(payload))
                            .status(OutboxStatus.NEW)
                            .createdAt(Instant.now())
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to save outbox command", e);
        }
    }
}
