package org.vippro.saga_service.service;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.vippro.event.PaymentInitiated;
import org.vippro.saga_service.model.PaymentState;
import org.vippro.saga_service.model.StepStatus;
import org.vippro.saga_service.model.SagaState;
import org.vippro.saga_service.repository.SagaStateRepository;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SagaStateService {
    private final SagaStateRepository sagaStateRepository;

    @Transactional
    public SagaState start(PaymentInitiated event) {
        return sagaStateRepository.findByPaymentId(event.getPaymentId())
                .orElseGet(() -> sagaStateRepository.save(
                        SagaState.builder()
                                .paymentId(event.getPaymentId())
                                .sourceAccountId(event.getSourceAccountId())
                                .destinationAccountId(event.getDestinationAccountId())
                                .amount(event.getAmount())
                                .currency(event.getCurrency())
                                .correlationId(event.getCorrelationId())
                                .paymentState(PaymentState.PROCESSING)
                                .fraudStatus(StepStatus.NOT_STARTED)
                                .debitStatus(StepStatus.NOT_STARTED)
                                .ledgerStatus(StepStatus.NOT_STARTED)
                                .debitReversalStatus(StepStatus.NOT_STARTED)
                                .build()
                ));
    }

    @Transactional(readOnly = true)
    public SagaState find(UUID paymentId) {

        return sagaStateRepository.findByPaymentId(paymentId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Saga not found for paymentId=" + paymentId
                        )
                );
    }

    @Transactional
    public void fail(SagaState saga, String reason) {

        if (saga.getPaymentState() == PaymentState.COMPLETED ||
                saga.getPaymentState() == PaymentState.COMPENSATED) {
            return;
        }

        saga.setPaymentState(PaymentState.FAILED);
        saga.setLastError(reason);

        sagaStateRepository.save(saga);
    }

    @Transactional
    public void failDebit(SagaState saga, String reason) {

        if (saga.getDebitStatus() == StepStatus.FAILED ||
                saga.getDebitStatus() == StepStatus.COMPLETED) {
            return; // idempotency
        }

        saga.setDebitStatus(StepStatus.FAILED);
        saga.setLastError(reason);

        saga.setUpdatedAt(Instant.now());

        sagaStateRepository.save(saga);
    }

    @Transactional
    public void startCompensation(SagaState saga) {

        if (saga.getPaymentState() == PaymentState.COMPENSATED ||
                saga.getPaymentState() == PaymentState.CANCELLED) {
            return;
        }

        saga.setPaymentState(PaymentState.COMPENSATING);

        saga.setUpdatedAt(Instant.now());

        sagaStateRepository.save(saga);
    }

    @Transactional
    public void startDebitReversal(SagaState saga) {
        if (saga.getDebitReversalStatus() == StepStatus.PENDING
                || saga.getDebitReversalStatus() == StepStatus.COMPLETED) {
            return;
        }

        if (saga.getDebitStatus() != StepStatus.COMPLETED
                || saga.getDebitTransactionId() == null) {
            throw new IllegalStateException(
                    "Cannot reverse debit before a completed debit transaction"
            );
        }

        saga.setPaymentState(PaymentState.COMPENSATING);
        saga.setDebitReversalStatus(StepStatus.PENDING);
        sagaStateRepository.save(saga);
    }

    @Transactional
    public void completeDebitReversal(
            SagaState saga,
            UUID reversalTransactionId
    ) {
        if (reversalTransactionId == null) {
            throw new IllegalArgumentException(
                    "Reversal transactionId must not be null"
            );
        }

        if (saga.getDebitReversalStatus() != StepStatus.PENDING) {
            throw new IllegalStateException(
                    "Debit reversal is not pending"
            );
        }

        saga.setDebitReversalStatus(StepStatus.COMPLETED);
        saga.setReversalTransactionId(reversalTransactionId);
        sagaStateRepository.save(saga);
    }

    @Transactional
    public void cancel(SagaState saga) {

        if (saga.getPaymentState() == PaymentState.COMPLETED ||
                saga.getPaymentState() == PaymentState.CANCELLED) {
            return;
        }

        saga.setPaymentState(PaymentState.CANCELLED);

        sagaStateRepository.save(saga);
    }

    @Transactional
    public void markFraudPending(SagaState saga) {

        if (saga.getFraudStatus() != StepStatus.NOT_STARTED) {
            return;
        }

        saga.setFraudStatus(StepStatus.PENDING);

        sagaStateRepository.save(saga);
    }

    @Transactional
    public void markDebitPending(SagaState saga) {

        if (saga.getFraudStatus() != StepStatus.COMPLETED) {
            throw new IllegalStateException("Fraud not completed");
        }

        saga.setDebitStatus(StepStatus.PENDING);

        sagaStateRepository.save(saga);
    }

    @Transactional
    public void markLedgerPending(SagaState saga) {

        if (saga.getLedgerStatus() == StepStatus.PENDING ||
                saga.getLedgerStatus() == StepStatus.COMPLETED) {
            return;
        }

        if (saga.getDebitStatus() != StepStatus.COMPLETED) {
            throw new IllegalStateException(
                    "Cannot move to ledger before debit completed"
            );
        }

        saga.setLedgerStatus(StepStatus.PENDING);
        saga.setPaymentState(PaymentState.PROCESSING);

        sagaStateRepository.save(saga);
    }

    @Transactional
    public void completeFraud(SagaState saga) {

        saga.setFraudStatus(StepStatus.COMPLETED);

        sagaStateRepository.save(saga);
    }

    @Transactional
    public void completeLedger(SagaState saga) {

        saga.setLedgerStatus(StepStatus.COMPLETED);
        sagaStateRepository.save(saga);
    }

    @Transactional
    public void completePayment(SagaState saga) {
        if (saga.getPaymentState() == PaymentState.COMPLETED) {
            return;
        }

        if (saga.getLedgerStatus() != StepStatus.COMPLETED) {
            throw new IllegalStateException(
                    "Cannot complete payment before ledger completed"
            );
        }

        saga.setPaymentState(PaymentState.COMPLETED);
        sagaStateRepository.save(saga);
    }

    @Transactional
    public void completeDebit(SagaState saga, UUID transactionId) {

        saga.setDebitStatus(StepStatus.COMPLETED);
        saga.setDebitTransactionId(transactionId);

        sagaStateRepository.save(saga);
    }

    @Transactional
    public void markCompensated(SagaState saga) {

        saga.setPaymentState(PaymentState.COMPENSATED);

        sagaStateRepository.save(saga);
    }

    @Transactional
    public void failFraud(SagaState saga, String reason) {
        if (saga.getFraudStatus() == StepStatus.FAILED
                || saga.getFraudStatus() == StepStatus.COMPLETED) {
            return;
        }

        saga.setFraudStatus(StepStatus.FAILED);
        saga.setLastError(reason);
        sagaStateRepository.save(saga);
    }

    @Transactional
    public void failLedger(SagaState saga, String reason) {
        if (saga.getLedgerStatus() == StepStatus.COMPLETED
                || saga.getLedgerStatus() == StepStatus.FAILED) {
            return;
        }

        saga.setLedgerStatus(StepStatus.FAILED);
        saga.setLastError(reason);
        sagaStateRepository.save(saga);
    }
}
