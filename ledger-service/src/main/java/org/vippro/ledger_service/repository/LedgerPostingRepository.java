package org.vippro.ledger_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.vippro.ledger_service.model.LedgerPosting;

import java.util.List;
import java.util.UUID;

public interface LedgerPostingRepository
        extends JpaRepository<LedgerPosting, UUID> {

    List<LedgerPosting> findByJournalEntryIdOrderBySide(
            UUID journalEntryId
    );
}
