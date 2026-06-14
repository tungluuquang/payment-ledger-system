package org.vippro.saga_service.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.vippro.event.PaymentCancelled;
import org.vippro.saga_service.model.PaymentState;
import org.vippro.saga_service.model.SagaState;
import org.vippro.saga_service.model.StepStatus;
import org.vippro.saga_service.service.ProcessedEventService;
import org.vippro.saga_service.service.SagaStateService;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class PaymentCancelledHandler implements SagaEventHandler<PaymentCancelled> {

    private final SagaStateService sagaStateService;
    private final ProcessedEventService processedEventService;

    @Override
    public Class<PaymentCancelled> eventType() {
        return PaymentCancelled.class;
    }

    @Override
    @Transactional
    public void handle(PaymentCancelled event) {
        if (!processedEventService.tryProcess(event.getEventId(), event.getClass().getSimpleName())) {
            return;
        }

        SagaState saga = sagaStateService.find(event.getPaymentId());

        if (saga.getPaymentState() == PaymentState.CANCELLED) {
            return;
        }

        if (saga.getPaymentState() == PaymentState.COMPLETED
                || saga.getPaymentState() == PaymentState.COMPENSATED) {
            return;
        }

        if (!Objects.equals(saga.getCorrelationId(), event.getCorrelationId())) {
            throw new IllegalStateException(
                    "PaymentCancelled does not match saga"
            );
        }

        validateEvent(saga, event);

        if (saga.getPaymentState() == PaymentState.COMPENSATING) {
            if (saga.getDebitReversalStatus() != StepStatus.COMPLETED) {
                throw new IllegalStateException(
                        "Cannot finish compensation before debit reversal completed"
                );
            }

            sagaStateService.markCompensated(saga);
            return;
        }

        sagaStateService.cancel(saga);
    }

    private void validateEvent(SagaState saga, PaymentCancelled event) {
        if (!Objects.equals(saga.getPaymentId(), event.getPaymentId())
                || !Objects.equals(saga.getCorrelationId(), event.getCorrelationId())) {
            throw new IllegalStateException(
                    "PaymentCancelled does not match saga"
            );
        }
    }
}
