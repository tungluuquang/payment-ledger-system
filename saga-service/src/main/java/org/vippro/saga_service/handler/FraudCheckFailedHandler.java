package org.vippro.saga_service.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.vippro.event.FraudCheckFailed;
import org.vippro.saga_service.model.PaymentState;
import org.vippro.saga_service.model.SagaState;
import org.vippro.saga_service.model.StepStatus;
import org.vippro.saga_service.service.SagaStateService;

@Component
@RequiredArgsConstructor
public class FraudCheckFailedHandler implements SagaEventHandler<FraudCheckFailed>{

    private final SagaStateService sagaStateService;

    @Override
    public Class<FraudCheckFailed> eventType() {
        return FraudCheckFailed.class;
    }

    @Override
    public void handle(FraudCheckFailed event) {
        SagaState sagaState = sagaStateService.find(event.getPaymentId());

        if (sagaState.getPaymentState() == PaymentState.COMPENSATED ||
                sagaState.getFraudStatus() == StepStatus.FAILED) {
            return;
        }

    }
}
