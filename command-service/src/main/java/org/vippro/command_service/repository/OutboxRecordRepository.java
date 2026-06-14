package org.vippro.command_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.vippro.command_service.model.OutboxRecord;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxRecordRepository extends JpaRepository<OutboxRecord, UUID> {

    @Query(value = """
            SELECT *
            FROM outboxes
            WHERE outbox_status = 'NEW'
              AND next_retry_at <= now()
            ORDER BY created_at
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxRecord> lockNextBatch(@Param("limit") int limit);

    @Modifying
    @Query("""
            UPDATE OutboxRecord o
            SET o.outboxStatus = org.vippro.command_service.util.OutboxStatus.NEW,
                o.processingStartedAt = NULL,
                o.nextRetryAt = :now,
                o.lastError = :reason
            WHERE o.outboxStatus = org.vippro.command_service.util.OutboxStatus.PROCESSING
              AND o.processingStartedAt < :threshold
            """)
    int resetStuckProcessing(
            @Param("now") Instant now,
            @Param("threshold") Instant threshold,
            @Param("reason") String reason
    );
}
