package org.vippro.audit_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vippro.audit_service.model.AuditEvent;
import org.vippro.audit_service.repository.AuditEventStore;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class AuditIngestionService {

    private final AuditEventStore eventStore;
    private final AuditEventMetadataExtractor metadataExtractor;
    private final ObjectMapper objectMapper;
    private final Tracer tracer;
    private final MeterRegistry meterRegistry;

    @Transactional
    public void record(ConsumerRecord<String, Object> record) {
        Object event = record.value();
        AuditEventMetadata metadata = metadataExtractor.extract(event);
        String payload = serialize(event);
        Span currentSpan = tracer.currentSpan();
        AuditEvent auditEvent = AuditEvent.builder()
                .eventId(metadata.eventId())
                .paymentId(metadata.paymentId())
                .correlationId(metadata.correlationId())
                .eventType(event.getClass().getSimpleName())
                .sourceTopic(record.topic())
                .sourcePartition(record.partition())
                .sourceOffset(record.offset())
                .traceId(currentSpan == null
                        ? null
                        : currentSpan.context().traceId())
                .spanId(currentSpan == null
                        ? null
                        : currentSpan.context().spanId())
                .payload(payload)
                .contentHash(sha256(payload))
                .occurredAt(metadata.occurredAt())
                .recordedAt(Instant.now())
                .build();

        if (!eventStore.insertIfAbsent(auditEvent)) {
            meterRegistry.counter("audit.events.duplicates").increment();
            return;
        }

        Counter.builder("audit.events.recorded")
                .tag("event.type", event.getClass().getSimpleName())
                .tag("source.topic", record.topic())
                .register(meterRegistry)
                .increment();
    }

    private String serialize(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Could not serialize audit event",
                    e
            );
        }
    }

    private String sha256(String payload) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
