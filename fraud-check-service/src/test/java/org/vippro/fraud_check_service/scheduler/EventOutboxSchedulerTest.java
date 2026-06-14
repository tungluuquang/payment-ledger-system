package org.vippro.fraud_check_service.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.vippro.fraud_check_service.model.OutboxStatus;
import org.vippro.fraud_check_service.repository.EventOutboxRepository;

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

        verify(repository).deleteByStatusAndPublishedAtBefore(
                eq(OutboxStatus.PUBLISHED),
                any(Instant.class)
        );
    }
}
