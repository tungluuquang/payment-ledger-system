package org.vippro.saga_service.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.vippro.event.JournalEntryFailed;
import org.vippro.saga_service.model.PaymentState;
import org.vippro.saga_service.model.SagaState;
import org.vippro.saga_service.model.StepStatus;
import org.vippro.saga_service.service.OutboxCommandService;
import org.vippro.saga_service.service.ProcessedEventService;
import org.vippro.saga_service.service.SagaStateService;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class JournalEntryFailedHandler implements SagaEventHandler<JournalEntryFailed> {

    private final SagaStateService sagaStateService;
    private final ProcessedEventService processedEventService;
    private final OutboxCommandService outboxCommandService;


    @Override
    public Class<JournalEntryFailed> eventType() {
        return JournalEntryFailed.class;
    }

    @Override
    @Transactional
    public void handle(JournalEntryFailed event) {
        if (!processedEventService.tryProcess(event.getEventId(), event.getClass().getSimpleName())) {
            return;
        }

        SagaState saga = sagaStateService.find(event.getPaymentId());

        if (saga.getCreditReversalStatus() == StepStatus.PENDING
                || saga.getCreditReversalStatus() == StepStatus.COMPLETED
                || saga.getPaymentState() == PaymentState.COMPENSATING
                || saga.getPaymentState() == PaymentState.COMPENSATED
                || saga.getLedgerStatus() == StepStatus.COMPLETED) {
            return;
        }

        validateEvent(saga, event);

        sagaStateService.failLedger(saga, event.getReason());
        sagaStateService.startCreditReversal(saga);

        outboxCommandService.requestReverseCredit(
                saga,
                event.getReason()
        );
    }

    private void validateEvent(SagaState saga, JournalEntryFailed event) {
        if (!Objects.equals(saga.getSourceAccountId(), event.getDebitAccountId())
                || !Objects.equals(saga.getDestinationAccountId(), event.getCreditAccountId())
                || !Objects.equals(saga.getCorrelationId(), event.getCorrelationId())) {
            throw new IllegalStateException(
                    "JournalEntryFailed does not match saga"
            );
        }
    }

}
