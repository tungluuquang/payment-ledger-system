package org.vippro.fraud_check_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.vippro.fraud_check_service.model.EventOutbox;
import org.vippro.fraud_check_service.model.OutboxStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface EventOutboxRepository
        extends JpaRepository<EventOutbox, UUID> {

    @Query(value = """
            SELECT *
            FROM fraud_event_outbox
            WHERE status = 'NEW'
              AND next_retry_at <= now()
            ORDER BY created_at
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<EventOutbox> lockNextBatch(@Param("limit") int limit);

    @Modifying
    @Query("""
            UPDATE EventOutbox e
            SET e.status = org.vippro.fraud_check_service.model.OutboxStatus.NEW,
                e.processingStartedAt = NULL,
                e.nextRetryAt = :now,
                e.lastError = :reason
            WHERE e.status = org.vippro.fraud_check_service.model.OutboxStatus.PROCESSING
              AND e.processingStartedAt < :threshold
            """)
    int resetStuckProcessing(
            @Param("now") Instant now,
            @Param("threshold") Instant threshold,
            @Param("reason") String reason
    );

    long deleteByStatusAndPublishedAtBefore(
            OutboxStatus status,
            Instant threshold
    );
}
