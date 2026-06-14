package org.vippro.ledger_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.vippro.ledger_service.model.EventOutbox;
import org.vippro.ledger_service.model.OutboxStatus;
import org.vippro.ledger_service.repository.EventOutboxRepository;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventOutboxService {

    private static final String LEDGER_EVENTS_TOPIC = "ledger-events";

    private final EventOutboxRepository eventOutboxRepository;
    private final ObjectMapper objectMapper;

    public void save(UUID eventId, UUID aggregateId, Object event) {
        try {
            eventOutboxRepository.save(EventOutbox.builder()
                    .eventId(eventId)
                    .aggregateId(aggregateId)
                    .topic(LEDGER_EVENTS_TOPIC)
                    .eventType(event.getClass().getSimpleName())
                    .payload(objectMapper.writeValueAsString(event))
                    .status(OutboxStatus.NEW)
                    .retryCount(0)
                    .nextRetryAt(Instant.now())
                    .createdAt(Instant.now())
                    .build());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Could not serialize ledger event",
                    e
            );
        }
    }
}
