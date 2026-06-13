package org.vippro.saga_service.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "outbox_commands",
        indexes = {
                @Index(name = "idx_outbox_status", columnList = "status"),
                @Index(name = "idx_outbox_retry", columnList = "nextRetryAt")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxCommand {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false)
    private UUID sagaId;

    @Column(nullable = false)
    private String topic;

    @Column(nullable = false)
    private String commandType;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant processingStartedAt;

    private Instant publishedAt;

    @Column(nullable = false)
    private int retryCount;

    @Column(length = 2000)
    private String lastError;

    private Instant nextRetryAt;

    @Version
    private Long version;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();

        if (id == null)
            id = UUID.randomUUID();

        if (status == null)
            status = OutboxStatus.NEW;

        if (nextRetryAt == null)
            nextRetryAt = now;

        if (createdAt == null)
            createdAt = now;
    }
}