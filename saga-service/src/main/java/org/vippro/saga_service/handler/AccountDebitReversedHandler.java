package org.vippro.saga_service.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.vippro.event.AccountDebitReversed;
import org.vippro.saga_service.model.PaymentState;
import org.vippro.saga_service.model.SagaState;
import org.vippro.saga_service.model.StepStatus;
import org.vippro.saga_service.service.OutboxCommandService;
import org.vippro.saga_service.service.ProcessedEventService;
import org.vippro.saga_service.service.SagaStateService;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class AccountDebitReversedHandler
        implements SagaEventHandler<AccountDebitReversed> {

    private final SagaStateService sagaStateService;
    private final ProcessedEventService processedEventService;
    private final OutboxCommandService outboxCommandService;

    @Override
    public Class<AccountDebitReversed> eventType() {
        return AccountDebitReversed.class;
    }

    @Override
    @Transactional
    public void handle(AccountDebitReversed event) {
        if (!processedEventService.tryProcess(
                event.getEventId(),
                event.getClass().getSimpleName()
        )) {
            return;
        }

        SagaState saga = sagaStateService.find(event.getPaymentId());

        if (saga.getDebitReversalStatus() == StepStatus.COMPLETED) {
            return;
        }

        if (saga.getPaymentState() != PaymentState.COMPENSATING
                || saga.getDebitReversalStatus() != StepStatus.PENDING) {
            throw new IllegalStateException(
                    "AccountDebitReversed received before reversal was ready: reversalStatus="
                            + saga.getDebitReversalStatus()
                            + ", paymentState="
                            + saga.getPaymentState()
                            + ", sagaId="
                            + saga.getSagaId()
            );
        }

        validateEvent(saga, event);

        sagaStateService.completeDebitReversal(
                saga,
                event.getReversalTransactionId()
        );

        outboxCommandService.requestCancelPayment(
                saga,
                "Ledger entry failed; account debit reversed"
        );
    }

    private void validateEvent(
            SagaState saga,
            AccountDebitReversed event
    ) {
        if (event.getEventId() == null
                || event.getReversalTransactionId() == null
                || event.getAmount() == null
                || event.getCurrency() == null
                || !Objects.equals(saga.getPaymentId(), event.getPaymentId())
                || !Objects.equals(saga.getSourceAccountId(), event.getAccountId())
                || !Objects.equals(saga.getDebitTransactionId(), event.getOriginalTransactionId())
                || saga.getAmount().compareTo(event.getAmount()) != 0
                || saga.getCurrency() != event.getCurrency()
                || !Objects.equals(saga.getCorrelationId(), event.getCorrelationId())) {
            throw new IllegalStateException(
                    "AccountDebitReversed does not match saga"
            );
        }
    }
}
