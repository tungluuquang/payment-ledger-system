package org.vippro.ledger_service.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.vippro.event.JournalEntryFailed;
import org.vippro.event.JournalEntryRecorded;
import org.vippro.event.JournalEntryReversalRecorded;
import org.vippro.ledger_service.model.EventOutbox;
import org.vippro.ledger_service.model.OutboxStatus;
import org.vippro.ledger_service.repository.EventOutboxRepository;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventOutboxScheduler {

    private final EventOutboxRepository eventOutboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ledger.outbox.batch-size:100}")
    private int batchSize;

    @Value("${ledger.outbox.max-retries:5}")
    private int maxRetries;

    @Value("${ledger.outbox.processing-timeout-ms:300000}")
    private long processingTimeoutMs;

    @Value("${ledger.outbox.retention-days:7}")
    private long retentionDays;

    @Scheduled(
            fixedDelayString =
                    "${ledger.outbox.recovery-fixed-delay-ms:60000}"
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
            log.warn("Recovered {} stuck ledger outbox events", recovered);
        }
    }

    @Scheduled(fixedDelayString = "${ledger.outbox.fixed-delay-ms:2000}")
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
                        "Failed to publish ledger event {}",
                        outbox.getEventId(),
                        e
                );
            }
        }

        eventOutboxRepository.saveAll(events);
    }

    @Scheduled(
            fixedDelayString =
                    "${ledger.outbox.cleanup-fixed-delay-ms:3600000}"
    )
    @Transactional
    public void cleanupPublished() {
        long deleted = eventOutboxRepository
                .deleteByStatusAndPublishedAtBefore(
                        OutboxStatus.PUBLISHED,
                        Instant.now().minusSeconds(retentionDays * 86_400L)
                );
        if (deleted > 0) {
            log.info("Deleted {} published ledger outbox events", deleted);
        }
    }

    private Object deserialize(EventOutbox outbox) throws Exception {
        return switch (outbox.getEventType()) {
            case "JournalEntryRecorded" -> objectMapper.readValue(
                    outbox.getPayload(),
                    JournalEntryRecorded.class
            );
            case "JournalEntryFailed" -> objectMapper.readValue(
                    outbox.getPayload(),
                    JournalEntryFailed.class
            );
            case "JournalEntryReversalRecorded" -> objectMapper.readValue(
                    outbox.getPayload(),
                    JournalEntryReversalRecorded.class
            );
            default -> throw new IllegalArgumentException(
                    "Unknown ledger event type: " + outbox.getEventType()
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
