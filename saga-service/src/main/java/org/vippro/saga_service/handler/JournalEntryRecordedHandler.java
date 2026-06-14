package org.vippro.saga_service.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.vippro.event.JournalEntryRecorded;
import org.vippro.saga_service.model.PaymentState;
import org.vippro.saga_service.model.SagaState;
import org.vippro.saga_service.model.StepStatus;
import org.vippro.saga_service.service.OutboxCommandService;
import org.vippro.saga_service.service.ProcessedEventService;
import org.vippro.saga_service.service.SagaStateService;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class JournalEntryRecordedHandler implements SagaEventHandler<JournalEntryRecorded> {

    private final SagaStateService sagaStateService;
    private final ProcessedEventService processedEventService;
    private final OutboxCommandService outboxCommandService;

    @Override
    public Class<JournalEntryRecorded> eventType() {
        return JournalEntryRecorded.class;
    }

    @Override
    @Transactional
    public void handle(JournalEntryRecorded event) {
        if (!processedEventService.tryProcess(event.getEventId(), event.getClass().getSimpleName())) {
            return;
        }
        SagaState saga = sagaStateService.find(event.getPaymentId());
        if (saga.getLedgerStatus() == StepStatus.COMPLETED) {
            return;
        }

        if (saga.getLedgerStatus() != StepStatus.PENDING
                && saga.getLedgerStatus() != StepStatus.PROCESSING) {
            throw new IllegalStateException(
                    "JournalEntryRecorded received before ledger was ready: ledgerStatus="
                            + saga.getLedgerStatus()
                            + ", sagaId="
                            + saga.getSagaId()
            );
        }

        if (saga.getPaymentState() == PaymentState.CANCELLED
                || saga.getPaymentState() == PaymentState.COMPENSATING
                || saga.getPaymentState() == PaymentState.COMPENSATED) {
            return;
        }
        validateEvent(saga, event);

        sagaStateService.completeLedger(saga);
        outboxCommandService.requestCompletePayment(saga);

    }


    private void validateEvent(SagaState saga, JournalEntryRecorded event) {
        if (event.getAmount() == null
                || event.getCurrency() == null
                || !Objects.equals(saga.getSourceAccountId(), event.getDebitAccountId())
                || !Objects.equals(saga.getDestinationAccountId(), event.getCreditAccountId())
                || saga.getAmount().compareTo(event.getAmount()) != 0
                || saga.getCurrency() != event.getCurrency()
                || !Objects.equals(saga.getCorrelationId(), event.getCorrelationId())) {
            throw new IllegalStateException(
                    "JournalEntryRecorded does not match saga"
            );
        }
    }
}
