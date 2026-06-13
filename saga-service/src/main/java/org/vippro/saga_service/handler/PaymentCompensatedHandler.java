package org.vippro.saga_service.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.vippro.event.PaymentCompensated;
import org.vippro.saga_service.model.SagaState;
import org.vippro.saga_service.service.SagaStateService;

@Component
@RequiredArgsConstructor
public class PaymentCompensatedHandler implements SagaEventHandler<PaymentCompensated> {

    private final SagaStateService saga;

    @Override
    public Class<PaymentCompensated> eventType() {
        return PaymentCompensated.class;
    }

    @Override
    public void handle(PaymentCompensated event) {
        SagaState sagaState = saga.find(event.getPaymentId());

    }
}
