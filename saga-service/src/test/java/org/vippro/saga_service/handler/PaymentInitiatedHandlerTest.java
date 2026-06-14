package org.vippro.saga_service.handler;

import org.junit.jupiter.api.Test;
import org.vippro.event.PaymentInitiated;
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

class PaymentInitiatedHandlerTest {

    private final SagaStateService sagaStateService = mock(SagaStateService.class);
    private final ProcessedEventService processedEventService = mock(ProcessedEventService.class);
    private final OutboxCommandService outboxCommandService = mock(OutboxCommandService.class);
    private final PaymentInitiatedHandler handler = new PaymentInitiatedHandler(
            sagaStateService,
            processedEventService,
            outboxCommandService
    );

    @Test
    void doesNotEnqueueFraudCommandForExistingSaga() {
        PaymentInitiated event = event(new BigDecimal("100.00"));
        SagaState saga = saga(event, StepStatus.PENDING);

        when(processedEventService.tryProcess(
                event.getEventId(),
                PaymentInitiated.class.getSimpleName()
        )).thenReturn(true);
        when(sagaStateService.start(event)).thenReturn(saga);

        handler.handle(event);

        verify(sagaStateService, never()).markFraudPending(saga);
        verify(outboxCommandService, never()).requestFraudCheck(saga, event);
    }

    @Test
    void rejectsDuplicatePaymentIdWithDifferentBusinessData() {
        PaymentInitiated event = event(new BigDecimal("100.00"));
        SagaState saga = saga(event, StepStatus.PENDING);
        saga.setAmount(new BigDecimal("200.00"));

        when(processedEventService.tryProcess(
                event.getEventId(),
                PaymentInitiated.class.getSimpleName()
        )).thenReturn(true);
        when(sagaStateService.start(event)).thenReturn(saga);

        assertThrows(IllegalStateException.class, () -> handler.handle(event));

        verify(outboxCommandService, never()).requestFraudCheck(saga, event);
    }

    private PaymentInitiated event(BigDecimal amount) {
        return PaymentInitiated.builder()
                .eventId(UUID.randomUUID())
                .paymentId(UUID.randomUUID())
                .requesterUserId(UUID.randomUUID())
                .sourceAccountId(UUID.randomUUID())
                .destinationAccountId(UUID.randomUUID())
                .amount(amount)
                .currency(CurrencyType.USD)
                .correlationId(UUID.randomUUID())
                .idempotencyKey(UUID.randomUUID())
                .build();
    }

    private SagaState saga(PaymentInitiated event, StepStatus fraudStatus) {
        return SagaState.builder()
                .sagaId(UUID.randomUUID())
                .paymentId(event.getPaymentId())
                .requesterUserId(event.getRequesterUserId())
                .sourceAccountId(event.getSourceAccountId())
                .destinationAccountId(event.getDestinationAccountId())
                .amount(event.getAmount())
                .currency(event.getCurrency())
                .correlationId(event.getCorrelationId())
                .paymentState(PaymentState.PROCESSING)
                .fraudStatus(fraudStatus)
                .debitStatus(StepStatus.NOT_STARTED)
                .creditStatus(StepStatus.NOT_STARTED)
                .ledgerStatus(StepStatus.NOT_STARTED)
                .debitReversalStatus(StepStatus.NOT_STARTED)
                .creditReversalStatus(StepStatus.NOT_STARTED)
                .build();
    }
}
