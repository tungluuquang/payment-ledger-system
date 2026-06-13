package org.vippro.saga_service.handler;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.vippro.event.AccountDebited;
import org.vippro.event.FraudCheckPassed;
import org.vippro.saga_service.model.StepStatus;
import org.vippro.saga_service.model.SagaState;
import org.vippro.saga_service.service.OutboxCommandService;
import org.vippro.saga_service.service.SagaStateService;
import org.vippro.saga_service.service.ProcessedEventService;

import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
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

    if (saga.getLedgerStatus() == StepStatus.COMPLETED) {
        return;
    }

    if (saga.getDebitStatus() != StepStatus.PENDING
            && saga.getDebitStatus() != StepStatus.PROCESSING) {
        log.warn(
                "Ignoring AccountDebited: debitStatus={}, sagaId={}",
                saga.getDebitStatus(),
                saga.getSagaId()
        );
        return;
    }

    sagaStateService.completeDebit(saga);
    sagaStateService.markLedgerPending(saga);

    outboxCommandService.requestJournalEntry(saga, event);
    }

    private void validateEvent(SagaState saga, AccountDebited event) {
        if (!Objects.equals(saga.getSourceAccountId(), event.getAccountId())
                || !Objects.equals(saga.getCorrelationId(), event.getCorrelationId())) {
            throw new IllegalStateException(
                    "AccountDebited does not match saga"
            );
        }
    }
}
