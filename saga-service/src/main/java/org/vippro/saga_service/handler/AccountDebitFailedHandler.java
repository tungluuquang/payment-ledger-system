package org.vippro.saga_service.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.vippro.event.AccountDebitFailed;
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
public class AccountDebitFailedHandler implements SagaEventHandler<AccountDebitFailed> {

    private final SagaStateService sagaStateService;
    private final ProcessedEventService processedEventService;
    private final OutboxCommandService outboxCommandService;


    @Override
    public Class<AccountDebitFailed> eventType() {
        return AccountDebitFailed.class;
    }

    @Override
    @Transactional
    public void handle(AccountDebitFailed event) {
        if (!processedEventService.tryProcess(
                event.getEventId(),
                event.getClass().getSimpleName()
        )) {
            return;
        }

        SagaState saga =
                sagaStateService.find(event.getPaymentId());

        if (saga.getDebitStatus() == StepStatus.COMPLETED) {
            return;
        }

        if (saga.getDebitStatus() == StepStatus.FAILED
                || saga.getPaymentState() == PaymentState.COMPENSATED
                || saga.getPaymentState() == PaymentState.CANCELLED) {
            log.warn(
                    "Ignoring AccountDebited: debitStatus={}, sagaId={}",
                    saga.getDebitStatus(),
                    saga.getSagaId()
            );
            return;
        }

        validateEvent(saga, event);

        sagaStateService.failDebit(saga, event.getReason());
        sagaStateService.startCompensation(saga);

        outboxCommandService.requestCancelPayment(
                saga,
                event.getReason()
        );
    }

    private void validateEvent(SagaState saga, AccountDebitFailed event) {
        boolean matches =
                Objects.equals(saga.getSourceAccountId(), event.getAccountId())
                        && saga.getAmount().compareTo(event.getAmount()) == 0
                        && saga.getCurrency() == event.getCurrency()
                        && Objects.equals(
                        saga.getCorrelationId(),
                        event.getCorrelationId()
                );

        if (!matches) {
            throw new IllegalStateException(
                    "AccountDebitFailed does not match saga"
            );
        }
    }
}
