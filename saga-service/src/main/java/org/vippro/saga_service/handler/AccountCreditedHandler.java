package org.vippro.saga_service.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.vippro.event.AccountCredited;
import org.vippro.saga_service.model.SagaState;
import org.vippro.saga_service.model.StepStatus;
import org.vippro.saga_service.service.OutboxCommandService;
import org.vippro.saga_service.service.ProcessedEventService;
import org.vippro.saga_service.service.SagaStateService;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class AccountCreditedHandler implements SagaEventHandler<AccountCredited> {
    private final SagaStateService sagaStateService;
    private final OutboxCommandService outboxCommandService;
    private final ProcessedEventService processedEventService;

    @Override
    public Class<AccountCredited> eventType() {
        return AccountCredited.class;
    }

    @Override
    @Transactional
    public void handle(AccountCredited event) {
        if (!processedEventService.tryProcess(
                event.getEventId(), event.getClass().getSimpleName())) {
            return;
        }
        SagaState saga = sagaStateService.find(event.getPaymentId());
        if (saga.getCreditStatus() == StepStatus.COMPLETED) {
            return;
        }
        if (saga.getCreditStatus() != StepStatus.PENDING) {
            throw new IllegalStateException("Destination credit is not pending");
        }
        validate(saga, event);
        sagaStateService.completeCredit(saga, event.getTransactionId());
        sagaStateService.markLedgerPending(saga);
        outboxCommandService.requestJournalEntry(saga, event);
    }

    private void validate(SagaState saga, AccountCredited event) {
        if (event.getTransactionId() == null
                || !Objects.equals(saga.getDestinationAccountId(), event.getAccountId())
                || saga.getAmount().compareTo(event.getAmount()) != 0
                || saga.getCurrency() != event.getCurrency()
                || !Objects.equals(saga.getCorrelationId(), event.getCorrelationId())) {
            throw new IllegalStateException("AccountCredited does not match saga");
        }
    }
}
