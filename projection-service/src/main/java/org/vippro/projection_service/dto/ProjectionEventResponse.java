package org.vippro.projection_service.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.vippro.projection_service.model.ProjectionEvent;

import java.time.Instant;
import java.util.UUID;

public record ProjectionEventResponse(
        UUID eventId,
        String eventType,
        UUID correlationId,
        JsonNode payload,
        Instant occurredAt,
        Instant processedAt
) {
    public static ProjectionEventResponse from(
            ProjectionEvent event,
            ObjectMapper objectMapper
    ) {
        try {
            return new ProjectionEventResponse(
                    event.getEventId(),
                    event.getEventType(),
                    event.getCorrelationId(),
                    objectMapper.readTree(event.getPayload()),
                    event.getOccurredAt(),
                    event.getProcessedAt()
            );
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException(
                    "Stored projection event payload is invalid",
                    e
            );
        }
    }
}
