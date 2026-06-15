package org.vippro.account_service.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.vippro.account_service.entity.EventOutbox;
import org.vippro.account_service.repository.EventOutboxRepository;
import org.vippro.event.AccountCredited;
import org.vippro.event.AccountCreditFailed;
import org.vippro.event.AccountCreditReversed;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EventOutboxSchedulerTest {

    @Test
    void deserializesCreditEvents() throws Exception {
        ObjectMapper objectMapper =
                new ObjectMapper().findAndRegisterModules();
        EventOutboxScheduler scheduler = new EventOutboxScheduler(
                mock(EventOutboxRepository.class),
                mock(KafkaTemplate.class),
                objectMapper
        );
        UUID paymentId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-06-15T02:00:00Z");
        List<Object> events = List.of(
                AccountCredited.builder()
                        .eventId(eventId)
                        .paymentId(paymentId)
                        .accountId(accountId)
                        .transactionId(UUID.randomUUID())
                        .correlationId(correlationId)
                        .occurredAt(occurredAt)
                        .build(),
                AccountCreditFailed.builder()
                        .eventId(eventId)
                        .paymentId(paymentId)
                        .accountId(accountId)
                        .correlationId(correlationId)
                        .occurredAt(occurredAt)
                        .build(),
                AccountCreditReversed.builder()
                        .eventId(eventId)
                        .paymentId(paymentId)
                        .accountId(accountId)
                        .originalTransactionId(UUID.randomUUID())
                        .reversalTransactionId(UUID.randomUUID())
                        .correlationId(correlationId)
                        .occurredAt(occurredAt)
                        .build()
        );

        for (Object event : events) {
            EventOutbox outbox = EventOutbox.builder()
                    .eventType(event.getClass().getSimpleName())
                    .payload(objectMapper.writeValueAsString(event))
                    .build();

            assertInstanceOf(
                    event.getClass(),
                    scheduler.deserialize(outbox)
            );
        }
    }

    @Test
    void resetsProcessingRecordsOlderThanConfiguredTimeout() {
        EventOutboxRepository repository =
                mock(EventOutboxRepository.class);
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, Object> kafkaTemplate =
                mock(KafkaTemplate.class);
        EventOutboxScheduler scheduler = new EventOutboxScheduler(
                repository,
                kafkaTemplate,
                new ObjectMapper()
        );
        ReflectionTestUtils.setField(
                scheduler,
                "processingTimeoutMs",
                300_000L
        );

        when(repository.resetStuckProcessing(
                any(Instant.class),
                any(Instant.class),
                any(String.class)
        )).thenReturn(2);

        scheduler.recoverStuckProcessing();

        var nowCaptor =
                org.mockito.ArgumentCaptor.forClass(Instant.class);
        var thresholdCaptor =
                org.mockito.ArgumentCaptor.forClass(Instant.class);
        verify(repository).resetStuckProcessing(
                nowCaptor.capture(),
                thresholdCaptor.capture(),
                eq("Recovered stale PROCESSING outbox record")
        );

        long timeoutMs = nowCaptor.getValue().toEpochMilli()
                - thresholdCaptor.getValue().toEpochMilli();
        assertTrue(timeoutMs >= 300_000L);
    }
}
