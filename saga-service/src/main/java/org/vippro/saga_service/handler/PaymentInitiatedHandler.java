package org.vippro.saga_service.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.vippro.event.PaymentInitiated;
import org.vippro.saga_service.model.SagaState;
import org.vippro.saga_service.model.StepStatus;
import org.vippro.saga_service.service.OutboxCommandService;
import org.vippro.saga_service.service.ProcessedEventService;
import org.vippro.saga_service.service.SagaStateService;

import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
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
        validateEvent(event);

        if (!processedEventService.tryProcess(
                event.getEventId(),
                event.getClass().getSimpleName()
        )) {
            return;
        }

        SagaState saga = sagaStateService.start(event);
        validateSaga(saga, event);

        if (saga.getFraudStatus() != StepStatus.NOT_STARTED) {
            log.warn(
                    "Ignoring duplicate PaymentInitiated: fraudStatus={}, sagaId={}",
                    saga.getFraudStatus(),
                    saga.getSagaId()
            );
            return;
        }

        sagaStateService.markFraudPending(saga);

        outboxCommandService.requestFraudCheck(
                saga,
                event
        );
    }

    private void validateEvent(PaymentInitiated event) {
        if (event.getEventId() == null
                || event.getPaymentId() == null
                || event.getSourceAccountId() == null
                || event.getDestinationAccountId() == null
                || event.getAmount() == null
                || event.getAmount().signum() <= 0
                || event.getCurrency() == null
                || event.getCorrelationId() == null
                || event.getIdempotencyKey() == null) {
            throw new IllegalArgumentException(
                    "Invalid PaymentInitiated event"
            );
        }

        if (event.getSourceAccountId()
                .equals(event.getDestinationAccountId())) {
            throw new IllegalArgumentException(
                    "Source and destination accounts must differ"
            );
        }
    }

    private void validateSaga(SagaState saga, PaymentInitiated event) {
        if (!Objects.equals(saga.getPaymentId(), event.getPaymentId())
                || !Objects.equals(saga.getSourceAccountId(), event.getSourceAccountId())
                || !Objects.equals(saga.getDestinationAccountId(), event.getDestinationAccountId())
                || saga.getAmount().compareTo(event.getAmount()) != 0
                || saga.getCurrency() != event.getCurrency()
                || !Objects.equals(saga.getCorrelationId(), event.getCorrelationId())) {
            throw new IllegalStateException(
                    "PaymentInitiated does not match existing saga"
            );
        }
    }
}
