package org.vippro.analytics_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.vippro.analytics_service.model.AnalyticsPaymentStatus;
import org.vippro.analytics_service.repository.AnalyticsEventRepository;
import org.vippro.analytics_service.repository.PaymentAnalyticsRepository;
import org.vippro.event.*;
import org.vippro.util.CurrencyType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class PaymentAnalyticsProjectorIntegrationTest {

    private static final Instant BASE_TIME =
            Instant.parse("2026-06-15T00:00:00Z");

    @Autowired
    private PaymentAnalyticsProjector projector;

    @Autowired
    private PaymentAnalyticsRepository paymentRepository;

    @Autowired
    private AnalyticsEventRepository eventRepository;

    @BeforeEach
    void cleanDatabase() {
        eventRepository.deleteAll();
        paymentRepository.deleteAll();
    }

    @Test
    void projectsSuccessfulPaymentAndIgnoresDuplicateEvent() {
        PaymentData data = paymentData();
        PaymentInitiated initiated = initiated(data, UUID.randomUUID());
        projector.project(initiated);
        projector.project(AccountDebited.builder()
                .eventId(UUID.randomUUID())
                .paymentId(data.paymentId())
                .accountId(data.sourceAccountId())
                .amount(data.amount())
                .currency(data.currency())
                .correlationId(data.correlationId())
                .occurredAt(BASE_TIME.plusSeconds(2))
                .build());
        projector.project(PaymentCompleted.builder()
                .eventId(UUID.randomUUID())
                .paymentId(data.paymentId())
                .correlationId(data.correlationId())
                .occurredAt(BASE_TIME.plusSeconds(5))
                .build());
        projector.project(initiated);

        var payment = paymentRepository.findById(data.paymentId())
                .orElseThrow();
        assertThat(payment.getStatus())
                .isEqualTo(AnalyticsPaymentStatus.COMPLETED);
        assertThat(payment.getAmount()).isEqualByComparingTo(data.amount());
        assertThat(payment.getCurrency()).isEqualTo("USD");
        assertThat(payment.getCompletedAt())
                .isEqualTo(BASE_TIME.plusSeconds(5));
        assertThat(eventRepository.count()).isEqualTo(3);
    }

    @Test
    void preservesLatestTerminalEventWhenEventsArriveOutOfOrder() {
        PaymentData data = paymentData();
        projector.project(initiated(data, UUID.randomUUID()));
        projector.project(PaymentCompleted.builder()
                .eventId(UUID.randomUUID())
                .paymentId(data.paymentId())
                .correlationId(data.correlationId())
                .occurredAt(BASE_TIME.plusSeconds(10))
                .build());
        projector.project(PaymentCancelled.builder()
                .eventId(UUID.randomUUID())
                .paymentId(data.paymentId())
                .correlationId(data.correlationId())
                .reason("Late cancellation event")
                .occurredAt(BASE_TIME.plusSeconds(8))
                .build());

        var payment = paymentRepository.findById(data.paymentId())
                .orElseThrow();
        assertThat(payment.getStatus())
                .isEqualTo(AnalyticsPaymentStatus.COMPLETED);
        assertThat(payment.getLastEventType())
                .isEqualTo("PaymentCompleted");
    }

    @Test
    void capturesFailureStageBeforeCancellation() {
        PaymentData data = paymentData();
        projector.project(initiated(data, UUID.randomUUID()));
        projector.project(AccountCreditFailed.builder()
                .eventId(UUID.randomUUID())
                .paymentId(data.paymentId())
                .accountId(data.destinationAccountId())
                .amount(data.amount())
                .currency(data.currency())
                .correlationId(data.correlationId())
                .errorCode("ACCOUNT_NOT_ACTIVE")
                .reason("Destination account is blocked")
                .occurredAt(BASE_TIME.plusSeconds(3))
                .build());

        var payment = paymentRepository.findById(data.paymentId())
                .orElseThrow();
        assertThat(payment.getStatus())
                .isEqualTo(AnalyticsPaymentStatus.PROCESSING);
        assertThat(payment.getFailureStage()).isEqualTo("CREDIT");
        assertThat(payment.getFailureCode())
                .isEqualTo("ACCOUNT_NOT_ACTIVE");
    }

    private PaymentInitiated initiated(
            PaymentData data,
            UUID eventId
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
                .occurredAt(BASE_TIME)
                .build();
    }

    private PaymentData paymentData() {
        return new PaymentData(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("250.0000"),
                CurrencyType.USD
        );
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
