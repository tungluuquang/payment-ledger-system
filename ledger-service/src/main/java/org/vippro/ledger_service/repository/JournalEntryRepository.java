package org.vippro.ledger_service.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.vippro.ledger_service.model.JournalEntry;

import java.util.Optional;
import java.util.UUID;

public interface JournalEntryRepository
        extends JpaRepository<JournalEntry, UUID> {

    Optional<JournalEntry> findByIdempotencyKey(UUID idempotencyKey);

    Optional<JournalEntry> findByOriginalJournalEntryId(
            UUID originalJournalEntryId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT j
            FROM JournalEntry j
            WHERE j.journalEntryId = :journalEntryId
            """)
    Optional<JournalEntry> findByIdForUpdate(
            @Param("journalEntryId") UUID journalEntryId
    );
}
