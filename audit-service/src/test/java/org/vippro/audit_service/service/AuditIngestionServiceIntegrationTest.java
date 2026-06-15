package org.vippro.audit_service.service;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.vippro.audit_service.repository.AuditEventRepository;
import org.vippro.event.PaymentInitiated;
import org.vippro.util.CurrencyType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class AuditIngestionServiceIntegrationTest {

    @Autowired
    private AuditIngestionService ingestionService;

    @Autowired
    private AuditQueryService queryService;

    @Autowired
    private AuditEventRepository repository;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    @Test
    void recordsImmutableEventMetadataAndIgnoresDuplicateDelivery() {
        UUID eventId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();
        PaymentInitiated event = PaymentInitiated.builder()
                .eventId(eventId)
                .paymentId(paymentId)
                .requesterUserId(UUID.randomUUID())
                .sourceAccountId(UUID.randomUUID())
                .destinationAccountId(UUID.randomUUID())
                .amount(new BigDecimal("125.5000"))
                .currency(CurrencyType.USD)
                .correlationId(correlationId)
                .idempotencyKey(UUID.randomUUID())
                .occurredAt(Instant.parse("2026-06-15T05:00:00Z"))
                .build();
        ConsumerRecord<String, Object> record = new ConsumerRecord<>(
                "payment-events",
                1,
                17,
                paymentId.toString(),
                event
        );

        ingestionService.record(record);
        ingestionService.record(record);

        assertThat(repository.count()).isEqualTo(1);
        var stored = repository.findById(eventId).orElseThrow();
        assertThat(stored.getPaymentId()).isEqualTo(paymentId);
        assertThat(stored.getCorrelationId()).isEqualTo(correlationId);
        assertThat(stored.getSourceTopic()).isEqualTo("payment-events");
        assertThat(stored.getSourcePartition()).isEqualTo(1);
        assertThat(stored.getSourceOffset()).isEqualTo(17);
        assertThat(stored.getContentHash()).hasSize(64);
        assertThat(stored.getPayload()).contains(paymentId.toString());

        var page = queryService.search(
                paymentId,
                correlationId,
                "PaymentInitiated",
                null,
                Instant.parse("2026-06-15T00:00:00Z"),
                Instant.parse("2026-06-16T00:00:00Z"),
                0,
                20
        );
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().getFirst().eventId())
                .isEqualTo(eventId);
    }
}
