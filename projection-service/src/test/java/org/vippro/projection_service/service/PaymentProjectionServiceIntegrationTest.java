package org.vippro.projection_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.vippro.event.*;
import org.vippro.projection_service.model.PaymentProjection;
import org.vippro.projection_service.model.PaymentViewStatus;
import org.vippro.projection_service.model.StepViewStatus;
import org.vippro.projection_service.repository.PaymentProjectionRepository;
import org.vippro.projection_service.repository.ProjectionEventRepository;
import org.vippro.util.CurrencyType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class PaymentProjectionServiceIntegrationTest {

    private static final Instant BASE_TIME =
            Instant.parse("2026-01-01T00:00:00Z");

    @Autowired
    private PaymentProjectionService projectionService;

    @Autowired
    private PaymentQueryService queryService;

    @Autowired
    private PaymentProjectionRepository projectionRepository;

    @Autowired
    private ProjectionEventRepository eventRepository;

    @BeforeEach
    void cleanDatabase() {
        eventRepository.deleteAll();
        projectionRepository.deleteAll();
    }

    @Test
    void projectsSuccessfulPaymentAndTimeline() {
        PaymentData data = paymentData();
        UUID debitTransactionId = UUID.randomUUID();
        UUID journalEntryId = UUID.randomUUID();

        projectionService.project(initiated(data, UUID.randomUUID(), 0));
        projectionService.project(FraudCheckPassed.builder()
                .eventId(UUID.randomUUID())
                .paymentId(data.paymentId())
                .accountId(data.sourceAccountId())
                .correlationId(data.correlationId())
                .occurredAt(at(1))
                .build());
        projectionService.project(AccountDebited.builder()
                .eventId(UUID.randomUUID())
                .paymentId(data.paymentId())
                .accountId(data.sourceAccountId())
                .transactionId(debitTransactionId)
                .amount(data.amount())
                .currency(data.currency())
                .correlationId(data.correlationId())
                .occurredAt(at(2))
                .build());
        projectionService.project(JournalEntryRecorded.builder()
                .eventId(UUID.randomUUID())
                .journalEntryId(journalEntryId)
                .paymentId(data.paymentId())
                .debitAccountId(data.sourceAccountId())
                .creditAccountId(data.destinationAccountId())
                .correlationId(data.correlationId())
                .amount(data.amount())
                .currency(data.currency())
                .description("Payment")
                .version(1)
                .occurredAt(at(3))
                .build());
        projectionService.project(PaymentCompleted.builder()
                .eventId(UUID.randomUUID())
                .paymentId(data.paymentId())
                .correlationId(data.correlationId())
                .occurredAt(at(4))
                .build());

        var response = queryService.find(data.paymentId());
        assertThat(response.paymentStatus())
                .isEqualTo(PaymentViewStatus.COMPLETED);
        assertThat(response.fraudStatus())
                .isEqualTo(StepViewStatus.COMPLETED);
        assertThat(response.debitStatus())
                .isEqualTo(StepViewStatus.COMPLETED);
        assertThat(response.ledgerStatus())
                .isEqualTo(StepViewStatus.COMPLETED);
        assertThat(response.debitTransactionId())
                .isEqualTo(debitTransactionId);
        assertThat(response.journalEntryId()).isEqualTo(journalEntryId);
        assertThat(response.lastEventType())
                .isEqualTo("PaymentCompleted");

        var timeline = queryService.timeline(data.paymentId(), 0, 20);
        assertThat(timeline.getTotalElements()).isEqualTo(5);
        assertThat(timeline.getContent())
                .extracting(event -> event.eventType())
                .containsExactly(
                        "PaymentInitiated",
                        "FraudCheckPassed",
                        "AccountDebited",
                        "JournalEntryRecorded",
                        "PaymentCompleted"
                );
    }

    @Test
    void handlesOutOfOrderAndDuplicateEvents() {
        PaymentData data = paymentData();
        UUID accountEventId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        AccountDebited accountDebited = AccountDebited.builder()
                .eventId(accountEventId)
                .paymentId(data.paymentId())
                .accountId(data.sourceAccountId())
                .transactionId(transactionId)
                .amount(data.amount())
                .currency(data.currency())
                .correlationId(data.correlationId())
                .occurredAt(at(2))
                .build();

        projectionService.project(accountDebited);
        projectionService.project(initiated(data, UUID.randomUUID(), 0));
        projectionService.project(accountDebited);

        PaymentProjection projection = projectionRepository
                .findById(data.paymentId())
                .orElseThrow();
        assertThat(projection.getPaymentStatus())
                .isEqualTo(PaymentViewStatus.PROCESSING);
        assertThat(projection.getDebitStatus())
                .isEqualTo(StepViewStatus.COMPLETED);
        assertThat(projection.getSourceAccountId())
                .isEqualTo(data.sourceAccountId());
        assertThat(projection.getDestinationAccountId())
                .isEqualTo(data.destinationAccountId());
        assertThat(projection.getDebitTransactionId())
                .isEqualTo(transactionId);
        assertThat(eventRepository.count()).isEqualTo(2);
    }

    @Test
    void doesNotRegressCompletedStepWithLateFailure() {
        PaymentData data = paymentData();
        projectionService.project(initiated(data, UUID.randomUUID(), 0));
        projectionService.project(FraudCheckPassed.builder()
                .eventId(UUID.randomUUID())
                .paymentId(data.paymentId())
                .accountId(data.sourceAccountId())
                .correlationId(data.correlationId())
                .occurredAt(at(2))
                .build());
        projectionService.project(FraudCheckFailed.builder()
                .eventId(UUID.randomUUID())
                .paymentId(data.paymentId())
                .accountId(data.sourceAccountId())
                .correlationId(data.correlationId())
                .reason("Late timeout")
                .occurredAt(at(1))
                .build());

        PaymentProjection projection = projectionRepository
                .findById(data.paymentId())
                .orElseThrow();
        assertThat(projection.getFraudStatus())
                .isEqualTo(StepViewStatus.COMPLETED);
        assertThat(projection.getPaymentStatus())
                .isEqualTo(PaymentViewStatus.PROCESSING);
        assertThat(projection.getLastError()).isNull();
        assertThat(projection.getLastEventType())
                .isEqualTo("FraudCheckPassed");
    }

    @Test
    void rollsBackEventAndProjectionWhenCoreDataConflicts() {
        PaymentData data = paymentData();
        projectionService.project(initiated(data, UUID.randomUUID(), 0));

        UUID conflictingEventId = UUID.randomUUID();
        JournalEntryRecorded conflictingEvent =
                JournalEntryRecorded.builder()
                        .eventId(conflictingEventId)
                        .journalEntryId(UUID.randomUUID())
                        .paymentId(data.paymentId())
                        .debitAccountId(UUID.randomUUID())
                        .creditAccountId(data.destinationAccountId())
                        .correlationId(data.correlationId())
                        .amount(data.amount())
                        .currency(data.currency())
                        .occurredAt(at(1))
                        .build();

        assertThatThrownBy(
                () -> projectionService.project(conflictingEvent)
        ).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("source account");

        assertThat(eventRepository.existsById(conflictingEventId)).isFalse();
        assertThat(eventRepository.count()).isEqualTo(1);
        PaymentProjection projection = projectionRepository
                .findById(data.paymentId())
                .orElseThrow();
        assertThat(projection.getLedgerStatus())
                .isEqualTo(StepViewStatus.NOT_STARTED);
    }

    private PaymentInitiated initiated(
            PaymentData data,
            UUID eventId,
            long seconds
    ) {
        return PaymentInitiated.builder()
                .eventId(eventId)
                .paymentId(data.paymentId())
                .requesterUserId(UUID.randomUUID())
                .sourceAccountId(data.sourceAccountId())
                .destinationAccountId(data.destinationAccountId())
                .amount(data.amount())
                .currency(data.currency())
                .correlationId(data.correlationId())
                .idempotencyKey(UUID.randomUUID())
                .occurredAt(at(seconds))
                .build();
    }

    private PaymentData paymentData() {
        return new PaymentData(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("125.5000"),
                CurrencyType.USD
        );
    }

    private Instant at(long seconds) {
        return BASE_TIME.plusSeconds(seconds);
    }

    private record PaymentData(
            UUID paymentId,
            UUID correlationId,
            UUID sourceAccountId,
            UUID destinationAccountId,
            BigDecimal amount,
            CurrencyType currency
    ) {
    }
}
