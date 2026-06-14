package org.vippro.saga_service.handler;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.vippro.event.AccountDebited;
import org.vippro.saga_service.model.StepStatus;
import org.vippro.saga_service.model.SagaState;
import org.vippro.saga_service.service.OutboxCommandService;
import org.vippro.saga_service.service.SagaStateService;
import org.vippro.saga_service.service.ProcessedEventService;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class AccountDebitedHandler implements SagaEventHandler<AccountDebited> {

    private final SagaStateService sagaStateService;
    private final OutboxCommandService outboxCommandService;
    private final ProcessedEventService processedEventService;

    @Override
    public Class<AccountDebited> eventType() {
        return AccountDebited.class;

    }

    @Override
    @Transactional
    public void handle(AccountDebited event) {
        if (!processedEventService.tryProcess (
            event.getEventId(),
            event.getClass().getSimpleName())) {
            return;
        }

        SagaState saga = sagaStateService.find(event.getPaymentId());

        if (saga.getLedgerStatus() == StepStatus.COMPLETED
                || saga.getDebitStatus() == StepStatus.COMPLETED) {
            return;
        }

        if (saga.getDebitStatus() != StepStatus.PENDING
                && saga.getDebitStatus() != StepStatus.PROCESSING) {
            throw new IllegalStateException(
                    "AccountDebited received before debit was ready: debitStatus="
                            + saga.getDebitStatus()
                            + ", sagaId="
                            + saga.getSagaId()
            );
        }

        validateEvent(saga, event);
        sagaStateService.completeDebit(saga, event.getTransactionId());
        sagaStateService.markLedgerPending(saga);

        outboxCommandService.requestJournalEntry(saga, event);
    }

    private void validateEvent(SagaState saga, AccountDebited event) {
        if (event.getTransactionId() == null) {
            throw new IllegalStateException(
                    "Debit transactionId is required"
            );
        }

        if (event.getAmount() == null
                || event.getCurrency() == null
                || !Objects.equals(saga.getSourceAccountId(), event.getAccountId())
                || saga.getAmount().compareTo(event.getAmount()) != 0
                || saga.getCurrency() != event.getCurrency()
                || !Objects.equals(saga.getCorrelationId(), event.getCorrelationId())) {
            throw new IllegalStateException(
                    "AccountDebited does not match saga"
            );
        }
    }
}
