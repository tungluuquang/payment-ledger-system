package org.vippro.saga_service.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.vippro.event.*;
import org.vippro.saga_service.model.PaymentState;
import org.vippro.saga_service.model.SagaState;
import org.vippro.saga_service.model.StepStatus;
import org.vippro.saga_service.repository.SagaStateRepository;
import org.vippro.saga_service.service.OutboxCommandService;
import org.vippro.saga_service.service.ProcessedEventService;
import org.vippro.saga_service.service.SagaStateService;
import org.vippro.util.CurrencyType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PaymentSagaEndToEndTest {
    private final SagaStateRepository repository =
            mock(SagaStateRepository.class);
    private final ProcessedEventService processedEvents =
            mock(ProcessedEventService.class);
    private final OutboxCommandService outbox =
            mock(OutboxCommandService.class);
    private final AtomicReference<SagaState> stored =
            new AtomicReference<>();
    private SagaStateService stateService;
    private Scenario scenario;

    @BeforeEach
    void setUp() {
        stateService = new SagaStateService(repository);
        when(repository.findByPaymentId(any())).thenAnswer(invocation ->
                Optional.ofNullable(stored.get())
        );
        when(repository.save(any())).thenAnswer(invocation -> {
            SagaState saga = invocation.getArgument(0);
            if (saga.getSagaId() == null) {
                saga.setSagaId(UUID.randomUUID());
            }
            stored.set(saga);
            return saga;
        });
        when(processedEvents.tryProcess(any(), any())).thenReturn(true);
        scenario = new Scenario();
    }

    @Test
    void successfulTransferCreditsDestinationBeforeLedgerCompletion() {
        startThroughCredit();

        new JournalEntryRecordedHandler(stateService, processedEvents, outbox)
                .handle(scenario.journalRecorded());
        new PaymentCompletedHandler(stateService, processedEvents)
                .handle(scenario.paymentCompleted());

        SagaState saga = stored.get();
        assertEquals(StepStatus.COMPLETED, saga.getDebitStatus());
        assertEquals(StepStatus.COMPLETED, saga.getCreditStatus());
        assertEquals(StepStatus.COMPLETED, saga.getLedgerStatus());
        assertEquals(PaymentState.COMPLETED, saga.getPaymentState());

        var order = inOrder(outbox);
        order.verify(outbox).requestFraudCheck(any(), any());
        order.verify(outbox).requestAccountDebit(any(), any());
        order.verify(outbox).requestAccountCredit(any());
        order.verify(outbox).requestJournalEntry(any(), any());
        order.verify(outbox).requestCompletePayment(any());
    }

    @Test
    void ledgerFailureReversesCreditThenDebitAndCompensatesPayment() {
        startThroughCredit();

        new JournalEntryFailedHandler(stateService, processedEvents, outbox)
                .handle(scenario.journalFailed());
        new AccountCreditReversedHandler(
                stateService, outbox, processedEvents
        ).handle(scenario.creditReversed());
        new AccountDebitReversedHandler(
                stateService, processedEvents, outbox
        ).handle(scenario.debitReversed());
        new PaymentCancelledHandler(stateService, processedEvents)
                .handle(scenario.paymentCancelled());

        SagaState saga = stored.get();
        assertEquals(StepStatus.COMPLETED, saga.getCreditReversalStatus());
        assertEquals(StepStatus.COMPLETED, saga.getDebitReversalStatus());
        assertEquals(PaymentState.COMPENSATED, saga.getPaymentState());

        var order = inOrder(outbox);
        order.verify(outbox).requestReverseCredit(any(), any());
        order.verify(outbox).requestReverseDebit(any(), any());
        order.verify(outbox).requestCancelPayment(any(), any());
    }

    private void startThroughCredit() {
        new PaymentInitiatedHandler(stateService, processedEvents, outbox)
                .handle(scenario.initiated());
        new FraudCheckPassedHandler(stateService, outbox, processedEvents)
                .handle(scenario.fraudPassed());
        new AccountDebitedHandler(stateService, outbox, processedEvents)
                .handle(scenario.debited());
        new AccountCreditedHandler(stateService, outbox, processedEvents)
                .handle(scenario.credited());
    }

    private class Scenario {
        final UUID paymentId = UUID.randomUUID();
        final UUID userId = UUID.randomUUID();
        final UUID source = UUID.randomUUID();
        final UUID destination = UUID.randomUUID();
        final UUID correlation = UUID.randomUUID();
        final UUID debitTransaction = UUID.randomUUID();
        final UUID creditTransaction = UUID.randomUUID();
        final BigDecimal amount = new BigDecimal("100.00");

        PaymentInitiated initiated() {
            return PaymentInitiated.builder()
                    .eventId(UUID.randomUUID()).paymentId(paymentId)
                    .requesterUserId(userId).sourceAccountId(source)
                    .destinationAccountId(destination).amount(amount)
                    .currency(CurrencyType.USD).correlationId(correlation)
                    .idempotencyKey(UUID.randomUUID()).occurredAt(Instant.now())
                    .build();
        }

        FraudCheckPassed fraudPassed() {
            return FraudCheckPassed.builder()
                    .eventId(UUID.randomUUID()).paymentId(paymentId)
                    .accountId(source).correlationId(correlation)
                    .occurredAt(Instant.now()).build();
        }

        AccountDebited debited() {
            return AccountDebited.builder()
                    .eventId(UUID.randomUUID()).paymentId(paymentId)
                    .accountId(source).transactionId(debitTransaction)
                    .amount(amount).currency(CurrencyType.USD)
                    .correlationId(correlation).occurredAt(Instant.now())
                    .build();
        }

        AccountCredited credited() {
            return AccountCredited.builder()
                    .eventId(UUID.randomUUID()).paymentId(paymentId)
                    .accountId(destination).transactionId(creditTransaction)
                    .amount(amount).currency(CurrencyType.USD)
                    .correlationId(correlation).occurredAt(Instant.now())
                    .build();
        }

        JournalEntryRecorded journalRecorded() {
            return JournalEntryRecorded.builder()
                    .eventId(UUID.randomUUID()).journalEntryId(UUID.randomUUID())
                    .paymentId(paymentId).debitAccountId(source)
                    .creditAccountId(destination).correlationId(correlation)
                    .amount(amount).currency(CurrencyType.USD)
                    .occurredAt(Instant.now()).build();
        }

        JournalEntryFailed journalFailed() {
            return JournalEntryFailed.builder()
                    .eventId(UUID.randomUUID()).paymentId(paymentId)
                    .debitAccountId(source).creditAccountId(destination)
                    .correlationId(correlation).reason("ledger unavailable")
                    .errorCode("LEDGER_ERROR").occurredAt(Instant.now())
                    .build();
        }

        AccountCreditReversed creditReversed() {
            return AccountCreditReversed.builder()
                    .eventId(UUID.randomUUID()).paymentId(paymentId)
                    .accountId(destination)
                    .originalTransactionId(creditTransaction)
                    .reversalTransactionId(UUID.randomUUID()).amount(amount)
                    .currency(CurrencyType.USD).correlationId(correlation)
                    .occurredAt(Instant.now()).build();
        }

        AccountDebitReversed debitReversed() {
            return AccountDebitReversed.builder()
                    .eventId(UUID.randomUUID()).paymentId(paymentId)
                    .accountId(source).originalTransactionId(debitTransaction)
                    .reversalTransactionId(UUID.randomUUID()).amount(amount)
                    .currency(CurrencyType.USD).correlationId(correlation)
                    .occurredAt(Instant.now()).build();
        }

        PaymentCompleted paymentCompleted() {
            return PaymentCompleted.builder().eventId(UUID.randomUUID())
                    .paymentId(paymentId).correlationId(correlation)
                    .occurredAt(Instant.now()).build();
        }

        PaymentCancelled paymentCancelled() {
            return PaymentCancelled.builder().eventId(UUID.randomUUID())
                    .paymentId(paymentId).correlationId(correlation)
                    .reason("compensated").occurredAt(Instant.now()).build();
        }
    }
}
