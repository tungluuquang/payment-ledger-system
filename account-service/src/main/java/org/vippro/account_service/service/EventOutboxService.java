package org.vippro.account_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.vippro.account_service.entity.EventOutbox;
import org.vippro.account_service.enums.EventOutboxStatus;
import org.vippro.account_service.repository.EventOutboxRepository;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventOutboxService {

    private static final String ACCOUNT_EVENTS_TOPIC = "account-events";

    private final EventOutboxRepository eventOutboxRepository;
    private final ObjectMapper objectMapper;

    public void save(UUID eventId, UUID accountId, Object event) {
        try {
            eventOutboxRepository.save(
                    EventOutbox.builder()
                            .eventId(eventId)
                            .aggregateId(accountId)
                            .topic(ACCOUNT_EVENTS_TOPIC)
                            .eventType(event.getClass().getSimpleName())
                            .payload(objectMapper.writeValueAsString(event))
                            .status(EventOutboxStatus.NEW)
                            .createdAt(Instant.now())
                            .nextRetryAt(Instant.now())
                            .build()
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Failed to serialize account event "
                            + event.getClass().getSimpleName(),
                    e
            );
        }
    }
}
