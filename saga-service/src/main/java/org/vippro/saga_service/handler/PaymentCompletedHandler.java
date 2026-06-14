package org.vippro.saga_service.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.vippro.event.PaymentCompleted;
import org.vippro.saga_service.model.PaymentState;
import org.vippro.saga_service.model.SagaState;
import org.vippro.saga_service.model.StepStatus;
import org.vippro.saga_service.service.ProcessedEventService;
import org.vippro.saga_service.service.SagaStateService;

import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentCompletedHandler implements SagaEventHandler<PaymentCompleted> {

    private final SagaStateService sagaStateService;
    private final ProcessedEventService processedEventService;

    @Override
    public Class<PaymentCompleted> eventType() {
        return PaymentCompleted.class;
    }

    @Override
    @Transactional
    public void handle(PaymentCompleted event) {
        if (!processedEventService.tryProcess(
                event.getEventId(),
                event.getClass().getSimpleName()
        )) {
            return;
        }

        SagaState saga = sagaStateService.find(event.getPaymentId());

        if (saga.getPaymentState() == PaymentState.COMPLETED) {
            return;
        }

        if (saga.getLedgerStatus() != StepStatus.COMPLETED
                || saga.getPaymentState() == PaymentState.CANCELLED
                || saga.getPaymentState() == PaymentState.COMPENSATING
                || saga.getPaymentState() == PaymentState.COMPENSATED) {
            log.warn(
                    "Ignoring PaymentCompleted: ledgerStatus={}, paymentState={}, sagaId={}",
                    saga.getLedgerStatus(),
                    saga.getPaymentState(),
                    saga.getSagaId()
            );
            return;
        }

        if (!Objects.equals(saga.getCorrelationId(), event.getCorrelationId())) {
            throw new IllegalStateException(
                    "PaymentCompleted does not match saga"
            );
        }

        sagaStateService.completePayment(saga);
    }
}
