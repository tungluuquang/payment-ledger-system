package org.vippro.command_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "events")
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

    private String aggregateType;
    private String eventType;

    @Column(columnDefinition = "TEXT")
    private String payload;

    private long version;
    private Instant occurredAt;

    private String serviceName;
    private String eventVersion;
}
