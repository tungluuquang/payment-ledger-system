package org.vippro.fraud_check_service.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.vippro.event.FraudCheckFailed;
import org.vippro.event.FraudCheckPassed;
import org.vippro.fraud_check_service.model.EventOutbox;
import org.vippro.fraud_check_service.model.OutboxStatus;
import org.vippro.fraud_check_service.repository.EventOutboxRepository;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventOutboxScheduler {

    private final EventOutboxRepository eventOutboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${fraud.outbox.batch-size:100}")
    private int batchSize;

    @Value("${fraud.outbox.max-retries:5}")
    private int maxRetries;

    @Value("${fraud.outbox.processing-timeout-ms:300000}")
    private long processingTimeoutMs;

    @Value("${fraud.outbox.retention-days:7}")
    private long retentionDays;

    @Scheduled(
            fixedDelayString =
                    "${fraud.outbox.recovery-fixed-delay-ms:60000}"
    )
    @Transactional
    public void recoverStuckProcessing() {
        Instant now = Instant.now();
        int recovered = eventOutboxRepository.resetStuckProcessing(
                now,
                now.minusMillis(processingTimeoutMs),
                "Recovered stale PROCESSING outbox record"
        );
        if (recovered > 0) {
            log.warn("Recovered {} stuck fraud outbox events", recovered);
        }
    }

    @Scheduled(fixedDelayString = "${fraud.outbox.fixed-delay-ms:2000}")
    @Transactional
    public void publish() {
        List<EventOutbox> events =
                eventOutboxRepository.lockNextBatch(batchSize);

        for (EventOutbox outbox : events) {
            try {
                outbox.setStatus(OutboxStatus.PROCESSING);
                outbox.setProcessingStartedAt(Instant.now());

                kafkaTemplate.send(
                        outbox.getTopic(),
                        outbox.getAggregateId().toString(),
                        deserialize(outbox)
                ).get();

                outbox.setStatus(OutboxStatus.PUBLISHED);
                outbox.setPublishedAt(Instant.now());
                outbox.setLastError(null);
            } catch (Exception e) {
                int retryCount = outbox.getRetryCount() + 1;
                outbox.setRetryCount(retryCount);
                outbox.setLastError(errorMessage(e));
                if (retryCount >= maxRetries) {
                    outbox.setStatus(OutboxStatus.FAILED);
                } else {
                    outbox.setStatus(OutboxStatus.NEW);
                    outbox.setNextRetryAt(
                            Instant.now().plusSeconds(backoffSeconds(retryCount))
                    );
                }
                log.error(
                        "Failed to publish fraud event {}",
                        outbox.getEventId(),
                        e
                );
            }
        }

        eventOutboxRepository.saveAll(events);
    }

    @Scheduled(
            fixedDelayString =
                    "${fraud.outbox.cleanup-fixed-delay-ms:3600000}"
    )
    @Transactional
    public void cleanupPublished() {
        long deleted = eventOutboxRepository
                .deleteByStatusAndPublishedAtBefore(
                        OutboxStatus.PUBLISHED,
                        Instant.now().minusSeconds(retentionDays * 86_400L)
                );
        if (deleted > 0) {
            log.info("Deleted {} published fraud outbox events", deleted);
        }
    }

    private Object deserialize(EventOutbox outbox) throws Exception {
        return switch (outbox.getEventType()) {
            case "FraudCheckPassed" -> objectMapper.readValue(
                    outbox.getPayload(),
                    FraudCheckPassed.class
            );
            case "FraudCheckFailed" -> objectMapper.readValue(
                    outbox.getPayload(),
                    FraudCheckFailed.class
            );
            default -> throw new IllegalArgumentException(
                    "Unknown fraud event type: " + outbox.getEventType()
            );
        };
    }

    private long backoffSeconds(int retryCount) {
        return Math.min(300L, 5L * (1L << Math.min(retryCount - 1, 6)));
    }

    private String errorMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null) {
            return exception.getClass().getSimpleName();
        }
        return message.length() <= 2000
                ? message
                : message.substring(0, 2000);
    }
}
