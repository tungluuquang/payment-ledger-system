package org.vippro.saga_service.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.vippro.event.FraudCheckPassed;
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
public class FraudCheckPassedHandler implements SagaEventHandler<FraudCheckPassed> {

    private final SagaStateService sagaStateService;
    private final OutboxCommandService outboxCommandService;
    private final ProcessedEventService processedEventService;

    @Override
    public Class<FraudCheckPassed> eventType() {
        return FraudCheckPassed.class;
    }

    @Override
    @Transactional
    public void handle(FraudCheckPassed event) {
        if (!processedEventService.tryProcess(
                event.getEventId(),
                event.getClass().getSimpleName())) {
            return;
        }

        SagaState saga = sagaStateService.find(event.getPaymentId());

        if (saga.getFraudStatus() == StepStatus.COMPLETED
                || saga.getDebitStatus() != StepStatus.NOT_STARTED
                || saga.getPaymentState() == PaymentState.CANCELLED
                || saga.getPaymentState() == PaymentState.COMPENSATED
                || saga.getPaymentState() == PaymentState.COMPLETED) {
            log.warn(
                    "Ignoring FraudCheckPassed: fraudStatus={}, debitStatus={}, paymentState={}, sagaId={}",
                    saga.getFraudStatus(),
                    saga.getDebitStatus(),
                    saga.getPaymentState(),
                    saga.getSagaId()
            );
            return;
        }

        validateEvent(saga, event);

        sagaStateService.completeFraud(saga);
        sagaStateService.markDebitPending(saga);

        outboxCommandService.requestAccountDebit(saga, event);

    }

    private void validateEvent(SagaState saga, FraudCheckPassed event) {
        if (!Objects.equals(
                saga.getSourceAccountId(),
                event.getAccountId()
        )
                || !Objects.equals(
                saga.getCorrelationId(),
                event.getCorrelationId()
        )) {
            throw new IllegalStateException(
                    "FraudCheckPassed does not match saga"
            );
        }
    }
}
