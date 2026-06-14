package org.vippro.command_service.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.vippro.command_service.repository.OutboxRecordRepository;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class OutboxSchedulerTest {

    @Test
    void resetsProcessingRecordsOlderThanConfiguredTimeout() {
        OutboxRecordRepository repository =
                mock(OutboxRecordRepository.class);
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, Object> kafkaTemplate =
                mock(KafkaTemplate.class);
        OutboxScheduler scheduler = new OutboxScheduler(
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

        ArgumentCaptor<Instant> nowCaptor =
                ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> thresholdCaptor =
                ArgumentCaptor.forClass(Instant.class);
        verify(repository).resetStuckProcessing(
                nowCaptor.capture(),
                thresholdCaptor.capture(),
                eq("Recovered stale PROCESSING outbox record")
        );
        assertTrue(
                nowCaptor.getValue().toEpochMilli()
                        - thresholdCaptor.getValue().toEpochMilli()
                        >= 300_000L
        );
    }
}
