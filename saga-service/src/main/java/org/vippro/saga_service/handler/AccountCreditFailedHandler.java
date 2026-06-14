package org.vippro.saga_service.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.vippro.event.AccountCreditFailed;
import org.vippro.saga_service.model.SagaState;
import org.vippro.saga_service.service.OutboxCommandService;
import org.vippro.saga_service.service.ProcessedEventService;
import org.vippro.saga_service.service.SagaStateService;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class AccountCreditFailedHandler
        implements SagaEventHandler<AccountCreditFailed> {
    private final SagaStateService sagaStateService;
    private final OutboxCommandService outboxCommandService;
    private final ProcessedEventService processedEventService;

    @Override
    public Class<AccountCreditFailed> eventType() {
        return AccountCreditFailed.class;
    }

    @Override
    @Transactional
    public void handle(AccountCreditFailed event) {
        if (!processedEventService.tryProcess(
                event.getEventId(), event.getClass().getSimpleName())) {
            return;
        }
        SagaState saga = sagaStateService.find(event.getPaymentId());
        if (!Objects.equals(saga.getDestinationAccountId(), event.getAccountId())
                || !Objects.equals(saga.getCorrelationId(), event.getCorrelationId())) {
            throw new IllegalStateException(
                    "AccountCreditFailed does not match saga"
            );
        }
        sagaStateService.failCredit(saga, event.getReason());
        sagaStateService.startDebitReversal(saga);
        outboxCommandService.requestReverseDebit(saga, event.getReason());
    }
}
