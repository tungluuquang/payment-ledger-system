package org.vippro.ledger_service.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.vippro.ledger_service.model.OutboxStatus;
import org.vippro.ledger_service.repository.EventOutboxRepository;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EventOutboxSchedulerTest {

    @Test
    void recoversStaleProcessingRecords() {
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
                any(), any(), anyString()
        )).thenReturn(2);

        scheduler.recoverStuckProcessing();

        ArgumentCaptor<Instant> now =
                ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> threshold =
                ArgumentCaptor.forClass(Instant.class);
        verify(repository).resetStuckProcessing(
                now.capture(),
                threshold.capture(),
                eq("Recovered stale PROCESSING outbox record")
        );
        assertTrue(
                now.getValue().toEpochMilli()
                        - threshold.getValue().toEpochMilli()
                        >= 300_000L
        );
    }

    @Test
    void deletesPublishedEventsPastRetention() {
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
        ReflectionTestUtils.setField(scheduler, "retentionDays", 7L);

        scheduler.cleanupPublished();

        ArgumentCaptor<Instant> threshold =
                ArgumentCaptor.forClass(Instant.class);
        verify(repository).deleteByStatusAndPublishedAtBefore(
                eq(OutboxStatus.PUBLISHED),
                threshold.capture()
        );
        assertTrue(
                threshold.getValue().isBefore(
                        Instant.now().minusSeconds(6L * 86_400L)
                )
        );
    }
}
