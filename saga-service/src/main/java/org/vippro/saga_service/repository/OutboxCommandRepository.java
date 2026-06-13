package org.vippro.saga_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.vippro.saga_service.model.OutboxCommand;
import org.vippro.saga_service.model.OutboxStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxCommandRepository extends JpaRepository<OutboxCommand, UUID> {
    @Modifying
    @Query("""
        UPDATE OutboxCommand o
        SET o.status = :status,
            o.processingStartedAt = :startedAt
        WHERE o.id IN :ids
    """)
    int markAsProcessing(
            @Param("ids") List<UUID> ids,
            @Param("status") OutboxStatus status,
            @Param("startedAt") Instant startedAt
    );

    @Query(value = """
        SELECT *
        FROM outbox_commands
        WHERE status = 'NEW'
          AND next_retry_at <= now()
        ORDER BY created_at
        LIMIT :limit
        FOR UPDATE SKIP LOCKED
        """,
            nativeQuery = true)
    List<OutboxCommand> lockNextBatch(
            @Param("limit") int limit
    );

    List<OutboxCommand>
    findByStatusAndProcessingStartedAtBefore(
            OutboxStatus status,
            Instant threshold
    );

    void deleteByStatusAndPublishedAtBefore(
            OutboxStatus status,
            Instant threshold
    );

    @Modifying
    @Query("""
UPDATE OutboxCommand o
SET o.status = 'NEW'
WHERE o.status = 'PROCESSING'
AND o.processingStartedAt < :threshold
""")
    int resetStuckProcessing(@Param("threshold") Instant threshold);

    @Modifying
    @Query("""
UPDATE OutboxCommand o
SET o.status = 'NEW',
    o.processingStartedAt = NULL,
    o.nextRetryAt = :now
WHERE o.status = 'PROCESSING'
  AND o.processingStartedAt < :threshold
""")
    int resetStuckProcessing(
            @Param("now") Instant now,
            @Param("threshold") Instant threshold
    );
}
