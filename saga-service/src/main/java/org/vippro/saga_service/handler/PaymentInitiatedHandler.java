package org.vippro.saga_service.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.vippro.event.PaymentInitiated;
import org.vippro.saga_service.model.SagaState;
import org.vippro.saga_service.service.OutboxCommandService;
import org.vippro.saga_service.service.ProcessedEventService;
import org.vippro.saga_service.service.SagaStateService;

@Component
@RequiredArgsConstructor
public class PaymentInitiatedHandler implements SagaEventHandler<PaymentInitiated> {

    private final SagaStateService sagaStateService;
    private final ProcessedEventService processedEventService;
    private final OutboxCommandService outboxCommandService;

    @Override
    public Class<PaymentInitiated> eventType() {
        return PaymentInitiated.class;
    }

    @Override
    @Transactional
    public void handle(PaymentInitiated event) {
        if (!processedEventService.tryProcess(
                event.getEventId(),
                event.getClass().getSimpleName()
        )) {
            return;
        }

        SagaState saga = sagaStateService.start(event);
        sagaStateService.markFraudPending(saga);

        outboxCommandService.requestFraudCheck(
                saga,
                event
        );
    }
}
