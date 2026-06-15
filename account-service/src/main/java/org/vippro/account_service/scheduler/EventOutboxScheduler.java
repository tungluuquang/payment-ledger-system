package org.vippro.account_service.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.vippro.account_service.entity.EventOutbox;
import org.vippro.account_service.enums.EventOutboxStatus;
import org.vippro.account_service.repository.EventOutboxRepository;
import org.vippro.event.AccountCredited;
import org.vippro.event.AccountCreditFailed;
import org.vippro.event.AccountCreditReversed;
import org.vippro.event.AccountDebitFailed;
import org.vippro.event.AccountDebitReversed;
import org.vippro.event.AccountDebited;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventOutboxScheduler {

    private static final int BATCH_SIZE = 100;
    private static final int MAX_RETRIES = 5;

    private final EventOutboxRepository eventOutboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${account.outbox.processing-timeout-ms:300000}")
    private long processingTimeoutMs;

    @Scheduled(
            fixedDelayString =
                    "${account.outbox.recovery-fixed-delay-ms:60000}"
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
            log.warn("Recovered {} stuck account outbox events", recovered);
        }
    }

    @Scheduled(fixedDelayString = "${account.outbox.fixed-delay-ms:2000}")
    @Transactional
    public void publish() {
        List<EventOutbox> events =
                eventOutboxRepository.lockNextBatch(BATCH_SIZE);

        for (EventOutbox outbox : events) {
            try {
                outbox.setStatus(EventOutboxStatus.PROCESSING);
                outbox.setProcessingStartedAt(Instant.now());

                Object event = deserialize(outbox);
                kafkaTemplate.send(
                        outbox.getTopic(),
                        outbox.getAggregateId().toString(),
                        event
                ).get();

                outbox.setStatus(EventOutboxStatus.PUBLISHED);
                outbox.setPublishedAt(Instant.now());
                outbox.setLastError(null);
            } catch (Exception e) {
                int retryCount = outbox.getRetryCount() + 1;
                outbox.setRetryCount(retryCount);
                outbox.setLastError(e.getMessage());

                if (retryCount >= MAX_RETRIES) {
                    outbox.setStatus(EventOutboxStatus.FAILED);
                } else {
                    outbox.setStatus(EventOutboxStatus.NEW);
                    outbox.setNextRetryAt(
                            Instant.now().plusSeconds(30L * retryCount)
                    );
                }

                log.error(
                        "Failed to publish account event {}",
                        outbox.getEventId(),
                        e
                );
            }
        }

        eventOutboxRepository.saveAll(events);
    }

    Object deserialize(EventOutbox outbox) throws Exception {
        return switch (outbox.getEventType()) {
            case "AccountDebited" -> objectMapper.readValue(
                    outbox.getPayload(),
                    AccountDebited.class
            );
            case "AccountDebitFailed" -> objectMapper.readValue(
                    outbox.getPayload(),
                    AccountDebitFailed.class
            );
            case "AccountDebitReversed" -> objectMapper.readValue(
                    outbox.getPayload(),
                    AccountDebitReversed.class
            );
            case "AccountCredited" -> objectMapper.readValue(
                    outbox.getPayload(),
                    AccountCredited.class
            );
            case "AccountCreditFailed" -> objectMapper.readValue(
                    outbox.getPayload(),
                    AccountCreditFailed.class
            );
            case "AccountCreditReversed" -> objectMapper.readValue(
                    outbox.getPayload(),
                    AccountCreditReversed.class
            );
            default -> throw new IllegalArgumentException(
                    "Unknown account event type: " + outbox.getEventType()
            );
        };
    }
}
