package org.vippro.saga_service.handler;

import org.junit.jupiter.api.Test;
import org.vippro.event.AccountDebitReversed;
import org.vippro.event.AccountDebited;
import org.vippro.event.JournalEntryRecorded;
import org.vippro.event.PaymentFailed;
import org.vippro.saga_service.model.PaymentState;
import org.vippro.saga_service.model.SagaState;
import org.vippro.saga_service.model.StepStatus;
import org.vippro.saga_service.service.OutboxCommandService;
import org.vippro.saga_service.service.ProcessedEventService;
import org.vippro.saga_service.service.SagaStateService;
import org.vippro.util.CurrencyType;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SagaEventValidationTest {

    private final SagaStateService sagaStateService = mock(SagaStateService.class);
    private final ProcessedEventService processedEventService = mock(ProcessedEventService.class);
    private final OutboxCommandService outboxCommandService = mock(OutboxCommandService.class);

    @Test
    void accountDebitedRejectsDifferentAmount() {
        SagaState saga = saga();
        AccountDebited event = AccountDebited.builder()
                .eventId(UUID.randomUUID())
                .paymentId(saga.getPaymentId())
                .accountId(saga.getSourceAccountId())
                .amount(new BigDecimal("99.00"))
                .currency(saga.getCurrency())
                .transactionId(UUID.randomUUID())
                .correlationId(saga.getCorrelationId())
                .build();

        when(processedEventService.tryProcess(
                event.getEventId(),
                AccountDebited.class.getSimpleName()
        )).thenReturn(true);
        when(sagaStateService.find(event.getPaymentId())).thenReturn(saga);

        AccountDebitedHandler handler = new AccountDebitedHandler(
                sagaStateService,
                outboxCommandService,
                processedEventService
        );

        assertThrows(IllegalStateException.class, () -> handler.handle(event));

        verify(sagaStateService, never()).completeDebit(saga, event.getTransactionId());
        verify(outboxCommandService, never()).requestAccountCredit(saga);
    }

    @Test
    void journalEntryRecordedRejectsDifferentCurrency() {
        SagaState saga = saga();
        saga.setDebitStatus(StepStatus.COMPLETED);
        saga.setLedgerStatus(StepStatus.PENDING);

        JournalEntryRecorded event = JournalEntryRecorded.builder()
                .eventId(UUID.randomUUID())
                .paymentId(saga.getPaymentId())
                .debitAccountId(saga.getSourceAccountId())
                .creditAccountId(saga.getDestinationAccountId())
                .amount(saga.getAmount())
                .currency(CurrencyType.EUR)
                .correlationId(saga.getCorrelationId())
                .build();

        when(processedEventService.tryProcess(
                event.getEventId(),
                JournalEntryRecorded.class.getSimpleName()
        )).thenReturn(true);
        when(sagaStateService.find(event.getPaymentId())).thenReturn(saga);

        JournalEntryRecordedHandler handler = new JournalEntryRecordedHandler(
                sagaStateService,
                processedEventService,
                outboxCommandService
        );

        assertThrows(IllegalStateException.class, () -> handler.handle(event));

        verify(sagaStateService, never()).completeLedger(saga);
        verify(outboxCommandService, never()).requestCompletePayment(saga);
    }

    @Test
    void paymentFailedDoesNotOverwriteCompensatingState() {
        SagaState saga = saga();
        saga.setPaymentState(PaymentState.COMPENSATING);
        PaymentFailed event = PaymentFailed.builder()
                .eventId(UUID.randomUUID())
                .paymentId(saga.getPaymentId())
                .correlationId(saga.getCorrelationId())
                .reason("late failure")
                .build();

        when(processedEventService.tryProcess(
                event.getEventId(),
                PaymentFailed.class.getSimpleName()
        )).thenReturn(true);
        when(sagaStateService.find(event.getPaymentId())).thenReturn(saga);

        PaymentFailedHandler handler = new PaymentFailedHandler(
                sagaStateService,
                processedEventService
        );

        handler.handle(event);

        verify(sagaStateService, never()).fail(saga, event.getReason());
    }

    @Test
    void accountDebitedThrowsWhenDebitIsNotReady() {
        SagaState saga = saga();
        saga.setDebitStatus(StepStatus.NOT_STARTED);
        AccountDebited event = AccountDebited.builder()
                .eventId(UUID.randomUUID())
                .paymentId(saga.getPaymentId())
                .accountId(saga.getSourceAccountId())
                .amount(saga.getAmount())
                .currency(saga.getCurrency())
                .transactionId(UUID.randomUUID())
                .correlationId(saga.getCorrelationId())
                .build();

        when(processedEventService.tryProcess(
                event.getEventId(),
                AccountDebited.class.getSimpleName()
        )).thenReturn(true);
        when(sagaStateService.find(event.getPaymentId())).thenReturn(saga);

        AccountDebitedHandler handler = new AccountDebitedHandler(
                sagaStateService,
                outboxCommandService,
                processedEventService
        );

        assertThrows(IllegalStateException.class, () -> handler.handle(event));

        verify(sagaStateService, never()).completeDebit(saga, event.getTransactionId());
        verify(outboxCommandService, never()).requestAccountCredit(saga);
    }

    @Test
    void journalEntryRecordedThrowsWhenLedgerIsNotReady() {
        SagaState saga = saga();
        saga.setDebitStatus(StepStatus.COMPLETED);
        saga.setLedgerStatus(StepStatus.NOT_STARTED);
        JournalEntryRecorded event = JournalEntryRecorded.builder()
                .eventId(UUID.randomUUID())
                .paymentId(saga.getPaymentId())
                .debitAccountId(saga.getSourceAccountId())
                .creditAccountId(saga.getDestinationAccountId())
                .amount(saga.getAmount())
                .currency(saga.getCurrency())
                .correlationId(saga.getCorrelationId())
                .build();

        when(processedEventService.tryProcess(
                event.getEventId(),
                JournalEntryRecorded.class.getSimpleName()
        )).thenReturn(true);
        when(sagaStateService.find(event.getPaymentId())).thenReturn(saga);

        JournalEntryRecordedHandler handler = new JournalEntryRecordedHandler(
                sagaStateService,
                processedEventService,
                outboxCommandService
        );

        assertThrows(IllegalStateException.class, () -> handler.handle(event));

        verify(sagaStateService, never()).completeLedger(saga);
        verify(outboxCommandService, never()).requestCompletePayment(saga);
    }

    @Test
    void accountDebitReversedThrowsWhenCompensationIsNotReady() {
        SagaState saga = saga();
        saga.setDebitStatus(StepStatus.COMPLETED);
        saga.setDebitTransactionId(UUID.randomUUID());
        AccountDebitReversed event = AccountDebitReversed.builder()
                .eventId(UUID.randomUUID())
                .paymentId(saga.getPaymentId())
                .accountId(saga.getSourceAccountId())
                .originalTransactionId(saga.getDebitTransactionId())
                .reversalTransactionId(UUID.randomUUID())
                .amount(saga.getAmount())
                .currency(saga.getCurrency())
                .correlationId(saga.getCorrelationId())
                .build();

        when(processedEventService.tryProcess(
                event.getEventId(),
                AccountDebitReversed.class.getSimpleName()
        )).thenReturn(true);
        when(sagaStateService.find(event.getPaymentId())).thenReturn(saga);

        AccountDebitReversedHandler handler = new AccountDebitReversedHandler(
                sagaStateService,
                processedEventService,
                outboxCommandService
        );

        assertThrows(IllegalStateException.class, () -> handler.handle(event));

        verify(sagaStateService, never()).completeDebitReversal(
                saga,
                event.getReversalTransactionId()
        );
        verify(outboxCommandService, never()).requestCancelPayment(
                saga,
                "Ledger entry failed; account debit reversed"
        );
    }

    private SagaState saga() {
        return SagaState.builder()
                .sagaId(UUID.randomUUID())
                .paymentId(UUID.randomUUID())
                .sourceAccountId(UUID.randomUUID())
                .destinationAccountId(UUID.randomUUID())
                .amount(new BigDecimal("100.00"))
                .currency(CurrencyType.USD)
                .correlationId(UUID.randomUUID())
                .paymentState(PaymentState.PROCESSING)
                .fraudStatus(StepStatus.COMPLETED)
                .debitStatus(StepStatus.PENDING)
                .creditStatus(StepStatus.NOT_STARTED)
                .ledgerStatus(StepStatus.NOT_STARTED)
                .debitReversalStatus(StepStatus.NOT_STARTED)
                .creditReversalStatus(StepStatus.NOT_STARTED)
                .build();
    }
}
