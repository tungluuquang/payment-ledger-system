//package org.vippro.saga_service.orchestrator;
//
//import jakarta.transaction.Transactional;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import org.vippro.event.*;
//import org.vippro.saga_service.messaging.producer.SagaCommandProducer;
//import org.vippro.saga_service.model.StepStatus;
//import org.vippro.saga_service.model.SagaState;
//import org.vippro.saga_service.repository.OutboxCommandRepository;
//import org.vippro.saga_service.repository.ProcessedEventRepository;
//import org.vippro.saga_service.repository.SagaStateRepository;
//import org.vippro.saga_service.service.OutboxCommandService;
//import org.vippro.saga_service.service.ProcessedEventService;
//import org.vippro.saga_service.service.SagaStateService;
//
//import java.util.UUID;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class PaymentSagaOrchestrator {
//    private final SagaStateRepository sagaStateRepository;
//    private final OutboxCommandRepository outboxCommandRepository;
//    private final SagaCommandProducer producer;
//    private final ProcessedEventRepository processedEventRepository;
//
//    private final SagaStateService sagaStateService;
//    private final ProcessedEventService processedEventService;
//    private final OutboxCommandService outboxCommandService;
//    @Transactional
//    public void on(PaymentInitiated event) {
//
//        if (!processedEventService.tryProcess(event.getEventId(), event.getClass().getSimpleName())) {
//            return;
//        }
//
//        SagaState saga = sagaStateService.start(event.getPaymentId());
//
//        outboxCommandService.requestFraudCheck(
//                saga.getSagaId(),
//                event
//        );
//    }
//
//    @Transactional
//    public void on(PaymentFailed event) {
//        if (!processedEventService.tryProcess(event.getEventId(), event.getClass().getSimpleName())) {
//            return;
//        }
//
//        SagaState saga = sagaStateService.find(event.getPaymentId());
//
//        sagaStateService.fail(saga, event.getReason());
//
//        log.warn(
//                "Payment saga FAILED. paymentId={}, reason={}",
//                event.getPaymentId(),
//                event.getReason()
//        );
//    }
//
//    @Transactional
//    public void on(PaymentCancelled event) {
//        if (!processedEventService.tryProcess(event.getEventId(), event.getClass().getSimpleName())) {
//            return;
//        }
//
//        SagaState saga = sagaStateService.find(event.getPaymentId());
//
//        sagaStateService.cancel(saga);
//    }
//
//    @Transactional
//    public void on(PaymentCompleted event) {
//        if (!processedEventService.tryProcess(event.getEventId(), event.getClass().getSimpleName())) {
//            return;
//        }
//
//        SagaState saga = sagaStateService.find(event.getPaymentId());
//
//        //sagaStateService.complete(saga);
//    }
//
//    @Transactional
//    public void on(FraudCheckPassed event) {
//        if (!processedEventService.tryProcess(event.getEventId(), event.getClass().getSimpleName())) {
//            return;
//        }
//
//        SagaState saga = sagaStateService.find(event.getPaymentId());
//
//        sagaStateService.markDebitPending(saga);
//    }
//
//    @Transactional
//    public void on(FraudCheckFailed event) {
//        if (!processedEventService.tryProcess(event.getEventId(), event.getClass().getSimpleName())) {
//            return;
//        }
//
//        SagaState saga = sagaStateService.find(event.getPaymentId());
//
//        sagaStateService.cancel(saga);
//    }
//
//    @Transactional
//    public void on(JournalEntryFailed event) {
//
//    }
//
//    @Transactional
//    public void on(JournalEntryRecorded event) {
//
//    }
//
//    @Transactional
//    public void on(AccountDebited event) {
//
//    }
//
//    @Transactional
//    public void on(AccountDebitFailed event) {
//
//    }
//
//    @Transactional
//    public void on(AccountDebitRequested event) {
//
//    }
//
//    @Transactional
//    public void on(JournalEntryReversalRecorded event) {
//
//    }
//
//    @Transactional
//    public void on(PaymentCompensated event) {
//
//    }
//
//    private SagaState createSaga(PaymentInitiated event) {
//        return SagaState.builder()
//                .sagaId(UUID.randomUUID())
//                .paymentId(event.getPaymentId())
//                .currentState(StepStatus.STARTED)
//                .build();
//    }
//}
