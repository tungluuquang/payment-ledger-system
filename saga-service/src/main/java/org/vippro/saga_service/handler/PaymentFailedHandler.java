package org.vippro.saga_service.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.vippro.event.PaymentFailed;
import org.vippro.saga_service.model.PaymentState;
import org.vippro.saga_service.model.SagaState;
import org.vippro.saga_service.service.ProcessedEventService;
import org.vippro.saga_service.service.SagaStateService;

import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentFailedHandler
        implements SagaEventHandler<PaymentFailed> {

    private final SagaStateService sagaStateService;
    private final ProcessedEventService processedEventService;

    @Override
    public Class<PaymentFailed> eventType() {
        return PaymentFailed.class;
    }

    @Override
    @Transactional
    public void handle(PaymentFailed event) {
        if (!processedEventService.tryProcess(
                event.getEventId(),
                event.getClass().getSimpleName()
        )) {
            return;
        }

        SagaState saga =
                sagaStateService.find(event.getPaymentId());

        if (saga.getPaymentState() == PaymentState.FAILED) {
            return;
        }

        if (saga.getPaymentState() == PaymentState.COMPLETED
                || saga.getPaymentState() == PaymentState.CANCELLED
                || saga.getPaymentState() == PaymentState.COMPENSATING
                || saga.getPaymentState() == PaymentState.COMPENSATED) {
            log.warn(
                    "Ignoring PaymentFailed: paymentState={}, sagaId={}",
                    saga.getPaymentState(),
                    saga.getSagaId()
            );
            return;
        }

        validateEvent(saga, event);

        sagaStateService.fail(saga, event.getReason());
    }

    private void validateEvent(
            SagaState saga,
            PaymentFailed event
    ) {
        if (!Objects.equals(
                saga.getPaymentId(),
                event.getPaymentId()
        )
                || !Objects.equals(
                saga.getCorrelationId(),
                event.getCorrelationId()
        )
                || event.getReason() == null
                || event.getReason().isBlank()) {
            throw new IllegalStateException(
                    "PaymentFailed does not match saga"
            );
        }
    }
}
