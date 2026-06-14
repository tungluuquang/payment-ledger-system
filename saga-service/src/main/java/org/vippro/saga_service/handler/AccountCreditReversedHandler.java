package org.vippro.saga_service.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.vippro.event.AccountCreditReversed;
import org.vippro.saga_service.model.SagaState;
import org.vippro.saga_service.model.StepStatus;
import org.vippro.saga_service.service.OutboxCommandService;
import org.vippro.saga_service.service.ProcessedEventService;
import org.vippro.saga_service.service.SagaStateService;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class AccountCreditReversedHandler
        implements SagaEventHandler<AccountCreditReversed> {
    private final SagaStateService sagaStateService;
    private final OutboxCommandService outboxCommandService;
    private final ProcessedEventService processedEventService;

    @Override
    public Class<AccountCreditReversed> eventType() {
        return AccountCreditReversed.class;
    }

    @Override
    @Transactional
    public void handle(AccountCreditReversed event) {
        if (!processedEventService.tryProcess(
                event.getEventId(), event.getClass().getSimpleName())) {
            return;
        }
        SagaState saga = sagaStateService.find(event.getPaymentId());
        if (saga.getCreditReversalStatus() == StepStatus.COMPLETED) {
            return;
        }
        if (!Objects.equals(saga.getDestinationAccountId(), event.getAccountId())
                || !Objects.equals(saga.getCreditTransactionId(),
                event.getOriginalTransactionId())
                || !Objects.equals(saga.getCorrelationId(), event.getCorrelationId())) {
            throw new IllegalStateException(
                    "AccountCreditReversed does not match saga"
            );
        }
        sagaStateService.completeCreditReversal(
                saga, event.getReversalTransactionId()
        );
        sagaStateService.startDebitReversal(saga);
        outboxCommandService.requestReverseDebit(
                saga, "Ledger entry failed; destination credit reversed"
        );
    }
}
