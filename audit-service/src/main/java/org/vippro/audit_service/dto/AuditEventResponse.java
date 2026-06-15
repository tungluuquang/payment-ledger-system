package org.vippro.audit_service.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.vippro.audit_service.model.AuditEvent;

import java.time.Instant;
import java.util.UUID;

public record AuditEventResponse(
        UUID eventId,
        UUID paymentId,
        UUID correlationId,
        String eventType,
        String sourceTopic,
        int sourcePartition,
        long sourceOffset,
        String traceId,
        String spanId,
        JsonNode payload,
        String contentHash,
        Instant occurredAt,
        Instant recordedAt
) {
    public static AuditEventResponse from(
            AuditEvent event,
            ObjectMapper objectMapper
    ) {
        try {
            return new AuditEventResponse(
                    event.getEventId(),
                    event.getPaymentId(),
                    event.getCorrelationId(),
                    event.getEventType(),
                    event.getSourceTopic(),
                    event.getSourcePartition(),
                    event.getSourceOffset(),
                    event.getTraceId(),
                    event.getSpanId(),
                    objectMapper.readTree(event.getPayload()),
                    event.getContentHash(),
                    event.getOccurredAt(),
                    event.getRecordedAt()
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Stored audit event payload is invalid",
                    e
            );
        }
    }
}
