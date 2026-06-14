package org.vippro.command_service.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.vippro.command_service.model.OutboxRecord;
import org.vippro.command_service.repository.OutboxRecordRepository;
import org.vippro.command_service.util.OutboxStatus;
import org.vippro.event.PaymentCancelled;
import org.vippro.event.PaymentCompleted;
import org.vippro.event.PaymentInitiated;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxScheduler {

    private static final int BATCH_SIZE = 100;
    private static final int MAX_RETRIES = 5;

    private final OutboxRecordRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${command.outbox.processing-timeout-ms:300000}")
    private long processingTimeoutMs;

    @Scheduled(
            fixedDelayString =
                    "${command.outbox.recovery-fixed-delay-ms:60000}"
    )
    @Transactional
    public void recoverStuckProcessing() {
        Instant now = Instant.now();
        int recovered = outboxRepository.resetStuckProcessing(
                now,
                now.minusMillis(processingTimeoutMs),
                "Recovered stale PROCESSING outbox record"
        );
        if (recovered > 0) {
            log.warn("Recovered {} stuck payment outbox events", recovered);
        }
    }

    @Scheduled(fixedDelayString = "${command.outbox.fixed-delay-ms:2000}")
    @Transactional
    public void publish() {
        List<OutboxRecord> records =
                outboxRepository.lockNextBatch(BATCH_SIZE);

        for (OutboxRecord outbox : records) {
            try {
                outbox.setOutboxStatus(OutboxStatus.PROCESSING);
                outbox.setProcessingStartedAt(Instant.now());

                kafkaTemplate.send(
                        outbox.getTopic(),
                        outbox.getAggregateId().toString(),
                        deserialize(outbox)
                ).get();

                outbox.setOutboxStatus(OutboxStatus.PUBLISHED);
                outbox.setPublishedAt(Instant.now());
                outbox.setLastError(null);
            } catch (Exception e) {
                int retryCount = outbox.getRetryCount() + 1;
                outbox.setRetryCount(retryCount);
                outbox.setLastError(errorMessage(e));

                if (retryCount >= MAX_RETRIES) {
                    outbox.setOutboxStatus(OutboxStatus.FAILED);
                } else {
                    outbox.setOutboxStatus(OutboxStatus.NEW);
                    outbox.setNextRetryAt(
                            Instant.now().plusSeconds(30L * retryCount)
                    );
                }
                log.error(
                        "Failed to publish payment event {}",
                        outbox.getEventId(),
                        e
                );
            }
        }

        outboxRepository.saveAll(records);
    }

    private Object deserialize(OutboxRecord outbox) throws Exception {
        return switch (outbox.getEventType()) {
            case "PaymentInitiated" -> objectMapper.readValue(
                    outbox.getPayload(),
                    PaymentInitiated.class
            );
            case "PaymentCompleted" -> objectMapper.readValue(
                    outbox.getPayload(),
                    PaymentCompleted.class
            );
            case "PaymentCancelled" -> objectMapper.readValue(
                    outbox.getPayload(),
                    PaymentCancelled.class
            );
            default -> throw new IllegalArgumentException(
                    "Unknown payment event type: " + outbox.getEventType()
            );
        };
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
