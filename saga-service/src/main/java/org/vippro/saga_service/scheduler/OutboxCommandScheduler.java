package org.vippro.saga_service.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.vippro.messaging.CommandEnvelope;
import org.vippro.saga_service.model.OutboxCommand;
import org.vippro.saga_service.repository.OutboxCommandRepository;
import org.vippro.saga_service.model.OutboxStatus;
import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxCommandScheduler {
    private final OutboxCommandRepository outbox;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final int BATCH_SIZE = 100;

    @Scheduled(fixedDelay = 2000)
    @Transactional
    public void process() {
        List<OutboxCommand> commands = outbox.lockNextBatch(BATCH_SIZE);

        if (commands.isEmpty())
            return;

        Instant now = Instant.now();

        for (OutboxCommand cmd : commands) {
            try {
                cmd.setStatus(OutboxStatus.PROCESSING);
                cmd.setProcessingStartedAt(now);

                CommandEnvelope envelope = CommandEnvelope.builder()
                        .commandId(cmd.getId())
                        .sagaId(cmd.getSagaId())
                        .commandType(cmd.getCommandType())
                        .payload(objectMapper.readTree(cmd.getPayload()))
                        .createdAt(cmd.getCreatedAt())
                        .build();

                String message = objectMapper.writeValueAsString(envelope);

                kafkaTemplate.send(
                        cmd.getTopic(),
                        cmd.getSagaId().toString(),
                        message
                ).get();

                cmd.setStatus(OutboxStatus.PUBLISHED);
                cmd.setPublishedAt(Instant.now());

                log.info("Published command {}", cmd.getId());
            } catch (Exception e) {
                log.error("Failed command {}", cmd.getId(), e);
                cmd.setRetryCount(cmd.getRetryCount() + 1);
                cmd.setLastError(e.getMessage());

                if (cmd.getRetryCount() >= 5) {
                    cmd.setStatus(OutboxStatus.FAILED);
                } else {
                    cmd.setStatus(OutboxStatus.NEW);
                    cmd.setNextRetryAt(
                            Instant.now().plusSeconds(30L * cmd.getRetryCount())
                    );
                }
            }
        }

        outbox.saveAll(commands);
    }
}
