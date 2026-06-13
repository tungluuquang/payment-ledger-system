package org.vippro.saga_service.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.vippro.event.FraudCheckPassed;
import org.vippro.saga_service.model.SagaState;
import org.vippro.saga_service.service.SagaStateService;

@Component
@RequiredArgsConstructor
public class FraudCheckPassedHandler implements SagaEventHandler<FraudCheckPassed> {

    private final SagaStateService saga;

    @Override
    public Class<FraudCheckPassed> eventType() {
        return FraudCheckPassed.class;
    }

    @Override
    public void handle(FraudCheckPassed event) {
        SagaState sagaState = saga.find(event.getPaymentId());

    }
}
