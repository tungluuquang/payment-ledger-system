package org.vippro.saga_service.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.vippro.event.FraudCheckFailed;
import org.vippro.saga_service.model.PaymentState;
import org.vippro.saga_service.model.SagaState;
import org.vippro.saga_service.model.StepStatus;
import org.vippro.saga_service.service.OutboxCommandService;
import org.vippro.saga_service.service.ProcessedEventService;
import org.vippro.saga_service.service.SagaStateService;

import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class FraudCheckFailedHandler implements SagaEventHandler<FraudCheckFailed>{

    private final SagaStateService sagaStateService;
    private final OutboxCommandService outboxCommandService;
    private final ProcessedEventService processedEventService;

    @Override
    public Class<FraudCheckFailed> eventType() {
        return FraudCheckFailed.class;
    }

    @Override
    @Transactional
    public void handle(FraudCheckFailed event) {
        if (!processedEventService.tryProcess(
                event.getEventId(),
                event.getClass().getSimpleName())) {
            return;
        }

        SagaState sagaState = sagaStateService.find(event.getPaymentId());

        if (sagaState.getFraudStatus() == StepStatus.COMPLETED
                || sagaState.getFraudStatus() == StepStatus.FAILED
                || sagaState.getPaymentState() == PaymentState.CANCELLED
                || sagaState.getPaymentState() == PaymentState.COMPENSATED
                || sagaState.getPaymentState() == PaymentState.COMPLETED) {
            log.warn(
                    "Ignoring FraudCheckFailed: fraudStatus={}, paymentState={}, sagaId={}",
                    sagaState.getFraudStatus(),
                    sagaState.getPaymentState(),
                    sagaState.getSagaId()
            );
            return;
        }

        validateEvent(sagaState, event);

        sagaStateService.failFraud(sagaState, event.getReason());

        outboxCommandService.requestCancelPayment(
                sagaState,
                event.getReason()
        );

    }

    private void validateEvent(SagaState saga, FraudCheckFailed event) {
        if (!Objects.equals(
                saga.getSourceAccountId(),
                event.getAccountId()
        )
                || !Objects.equals(
                saga.getCorrelationId(),
                event.getCorrelationId()
        )) {
            throw new IllegalStateException(
                    "FraudCheckFailed does not match saga"
            );
        }
    }
}
