package org.vippro.command_service.model;

import jakarta.persistence.*;
import lombok.*;
import org.vippro.command_service.util.OutboxStatus;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "outboxes",
        indexes = @Index(
                name = "idx_outbox_status_retry",
                columnList = "outbox_status,next_retry_at"
        )
)
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

    @Column(nullable = false, updatable = false)
    private String topic;

    @Column(nullable = false, updatable = false)
    private String eventType;

    @Column(columnDefinition = "TEXT", nullable = false, updatable = false)
    private String payload;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus outboxStatus;

    @Builder.Default
    private int retryCount = 0;

    @Column(nullable = false)
    private Instant nextRetryAt;

    private Instant processingStartedAt;
    private Instant publishedAt;

    @Column(length = 2000)
    private String lastError;

    @Version
    private long version;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (eventId == null) {
            eventId = id;
        }
        if (outboxStatus == null) {
            outboxStatus = OutboxStatus.NEW;
        }
        if (createdAt == null) {
            createdAt = now;
        }
        if (nextRetryAt == null) {
            nextRetryAt = now;
        }
    }
}
