package org.vippro.account_service.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.vippro.account_service.repository.EventOutboxRepository;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EventOutboxSchedulerTest {

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
