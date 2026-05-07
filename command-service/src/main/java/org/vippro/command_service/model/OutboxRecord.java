package org.vippro.command_service.model;

import jakarta.persistence.*;
import lombok.*;
import org.vippro.command_service.util.OutboxStatus;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "outboxes",  indexes = {
@   Index(name = "idx_outbox_status_created", columnList = "outbox_status, created_at")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class OutboxRecord {
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false)
    private UUID aggregateId;

    private UUID correlationId;
    private UUID eventId;
    private String eventType;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String payload;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private OutboxStatus outboxStatus;

    @Builder.Default
    private int retryCount = 0;
    private Instant nextRetryAt;
    private String lastError;
}
