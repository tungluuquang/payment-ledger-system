package org.vippro.saga_service.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.vippro.event.PaymentCancelled;
import org.vippro.saga_service.model.SagaState;
import org.vippro.saga_service.service.SagaStateService;

@Component
@RequiredArgsConstructor
public class PaymentCancelledHandler implements SagaEventHandler<PaymentCancelled> {

    private final SagaStateService saga;

    @Override
    public Class<PaymentCancelled> eventType() {
        return PaymentCancelled.class;
    }

    @Override
    public void handle(PaymentCancelled event) {
        SagaState sagaState = saga.find(event.getEventId());


    }
}
