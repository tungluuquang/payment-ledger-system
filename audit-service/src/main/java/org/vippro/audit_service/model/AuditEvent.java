package org.vippro.audit_service.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_events")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class AuditEvent {

    @Id
    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "payment_id", nullable = false, updatable = false)
    private UUID paymentId;

    @Column(name = "correlation_id", updatable = false)
    private UUID correlationId;

    @Column(name = "event_type", nullable = false, updatable = false)
    private String eventType;

    @Column(name = "source_topic", nullable = false, updatable = false)
    private String sourceTopic;

    @Column(name = "source_partition", nullable = false, updatable = false)
    private int sourcePartition;

    @Column(name = "source_offset", nullable = false, updatable = false)
    private long sourceOffset;

    @Column(name = "trace_id", length = 64, updatable = false)
    private String traceId;

    @Column(name = "span_id", length = 32, updatable = false)
    private String spanId;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT", updatable = false)
    private String payload;

    @Column(name = "content_hash", nullable = false, length = 64, updatable = false)
    private String contentHash;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;
}
