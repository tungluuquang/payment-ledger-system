package org.vippro.command_service.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "events",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_event_aggregate_version",
                columnNames = {"aggregate_id", "event_version_number"}
        ),
        indexes = @Index(
                name = "idx_event_aggregate",
                columnList = "aggregate_id,event_version_number"
        )
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class EventRecord {

    @Id
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private UUID eventId = UUID.randomUUID();

    @Column(nullable = false)
    private UUID aggregateId;
    private UUID correlationId;

    @Column(nullable = false)
    private String aggregateType;

    @Column(nullable = false)
    private String eventType;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String payload;

    @Column(name = "event_version_number", nullable = false)
    private long version;

    @Column(nullable = false)
    private Instant occurredAt;

    @Column(nullable = false)
    private String serviceName;

    @Column(nullable = false)
    private String eventVersion;

    @PrePersist
    void prePersist() {
        if (eventId == null) {
            eventId = UUID.randomUUID();
        }
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
    }
}
