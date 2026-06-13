package org.vippro.saga_service.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.vippro.event.AccountDebitFailed;
import org.vippro.saga_service.model.PaymentState;
import org.vippro.saga_service.model.SagaState;
import org.vippro.saga_service.model.StepStatus;
import org.vippro.saga_service.service.SagaStateService;

@Component
@RequiredArgsConstructor
public class AccountDebitFailedHandler implements SagaEventHandler<AccountDebitFailed> {

    private final SagaStateService sagaStateService;


    @Override
    public Class<AccountDebitFailed> eventType() {
        return AccountDebitFailed.class;
    }

    @Override
    public void handle(AccountDebitFailed event) {
        SagaState saga =
                sagaStateService.find(event.getPaymentId());

        if (saga.getDebitStatus() == StepStatus.FAILED ||
                saga.getPaymentState().equals(PaymentState.COMPENSATED)) {
            return;
        }

        sagaStateService.failDebit(saga, event.getReason());

        sagaStateService.startCompensation(saga);
    }
}
