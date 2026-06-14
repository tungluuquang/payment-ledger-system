package org.vippro.fraud_check_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.vippro.fraud_check_service.model.EventOutbox;
import org.vippro.fraud_check_service.model.OutboxStatus;
import org.vippro.fraud_check_service.repository.EventOutboxRepository;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventOutboxService {

    private static final String FRAUD_EVENTS_TOPIC = "fraud-events";

    private final EventOutboxRepository eventOutboxRepository;
    private final ObjectMapper objectMapper;

    public void save(UUID eventId, UUID aggregateId, Object event) {
        try {
            eventOutboxRepository.save(EventOutbox.builder()
                    .eventId(eventId)
                    .aggregateId(aggregateId)
                    .topic(FRAUD_EVENTS_TOPIC)
                    .eventType(event.getClass().getSimpleName())
                    .payload(objectMapper.writeValueAsString(event))
                    .status(OutboxStatus.NEW)
                    .retryCount(0)
                    .nextRetryAt(Instant.now())
                    .createdAt(Instant.now())
                    .build());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Could not serialize fraud event",
                    e
            );
        }
    }
}
