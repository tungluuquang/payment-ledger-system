package org.vippro.ledger_service.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.vippro.command.JournalEntryRequestedCommand;
import org.vippro.command.RecordReversalJournalEntryCommand;
import org.vippro.ledger_service.model.*;
import org.vippro.ledger_service.repository.EventOutboxRepository;
import org.vippro.ledger_service.repository.JournalEntryRepository;
import org.vippro.ledger_service.repository.LedgerPostingRepository;
import org.vippro.util.CurrencyType;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class LedgerCommandServiceIntegrationTest {

    @Autowired
    private LedgerCommandService service;

    @Autowired
    private JournalEntryRepository journalEntryRepository;

    @Autowired
    private LedgerPostingRepository postingRepository;

    @Autowired
    private EventOutboxRepository outboxRepository;

    @Test
    void recordsBalancedJournalAndSuppressesDuplicateCommand() {
        JournalEntryRequestedCommand command = recordCommand();
        UUID commandId = UUID.randomUUID();

        service.record(commandId, command);
        service.record(commandId, command);

        JournalEntry entry = journalEntryRepository
                .findByIdempotencyKey(command.getIdempotencyKey())
                .orElseThrow();
        List<LedgerPosting> postings = postingRepository
                .findByJournalEntryIdOrderBySide(entry.getJournalEntryId());

        assertEquals(JournalEntryType.PAYMENT, entry.getEntryType());
        assertEquals(2, postings.size());
        assertEquals(1, postings.stream()
                .filter(p -> p.getSide() == PostingSide.DEBIT)
                .count());
        assertEquals(1, postings.stream()
                .filter(p -> p.getSide() == PostingSide.CREDIT)
                .count());
        assertTrue(postings.stream().allMatch(
                p -> p.getAmount().compareTo(command.getAmount()) == 0
        ));
        assertEquals(1, outboxRepository.findAll().size());
        assertEquals(
                "JournalEntryRecorded",
                outboxRepository.findAll().getFirst().getEventType()
        );
    }

    @Test
    void createsReversalWithPostingSidesSwapped() {
        JournalEntryRequestedCommand originalCommand = recordCommand();
        service.record(UUID.randomUUID(), originalCommand);
        JournalEntry original = journalEntryRepository
                .findByIdempotencyKey(originalCommand.getIdempotencyKey())
                .orElseThrow();

        UUID reversalId = UUID.randomUUID();
        RecordReversalJournalEntryCommand reversalCommand =
                RecordReversalJournalEntryCommand.builder()
                        .journalEntryId(original.getJournalEntryId())
                        .reversalJournalEntryId(reversalId)
                        .paymentId(original.getPaymentId())
                        .debitAccountId(original.getDebitAccountId())
                        .creditAccountId(original.getCreditAccountId())
                        .amount(original.getAmount())
                        .currency(original.getCurrency())
                        .correlationId(original.getCorrelationId())
                        .idempotencyKey(UUID.randomUUID())
                        .build();

        service.reverse(UUID.randomUUID(), reversalCommand);

        JournalEntry reversal = journalEntryRepository
                .findById(reversalId)
                .orElseThrow();
        assertEquals(JournalEntryType.REVERSAL, reversal.getEntryType());
        assertEquals(original.getJournalEntryId(),
                reversal.getOriginalJournalEntryId());
        assertEquals(original.getCreditAccountId(),
                reversal.getDebitAccountId());
        assertEquals(original.getDebitAccountId(),
                reversal.getCreditAccountId());
        assertEquals(2, postingRepository
                .findByJournalEntryIdOrderBySide(reversalId)
                .size());
        assertEquals(2, outboxRepository.findAll().size());
    }

    @Test
    void emitsFailureWithoutCreatingJournalForInvalidAmount() {
        JournalEntryRequestedCommand valid = recordCommand();
        JournalEntryRequestedCommand invalid =
                JournalEntryRequestedCommand.builder()
                        .paymentId(valid.getPaymentId())
                        .debitAccountId(valid.getDebitAccountId())
                        .creditAccountId(valid.getCreditAccountId())
                        .correlationId(valid.getCorrelationId())
                        .idempotencyKey(valid.getIdempotencyKey())
                        .amount(BigDecimal.ZERO)
                        .currency(valid.getCurrency())
                        .build();

        service.record(UUID.randomUUID(), invalid);

        assertTrue(journalEntryRepository.findAll().isEmpty());
        assertEquals(1, outboxRepository.findAll().size());
        assertEquals(
                "JournalEntryFailed",
                outboxRepository.findAll().getFirst().getEventType()
        );
    }

    @Test
    void rejectsReversalThatDoesNotMatchOriginal() {
        JournalEntryRequestedCommand originalCommand = recordCommand();
        service.record(UUID.randomUUID(), originalCommand);
        JournalEntry original = journalEntryRepository
                .findByIdempotencyKey(originalCommand.getIdempotencyKey())
                .orElseThrow();

        RecordReversalJournalEntryCommand mismatch =
                RecordReversalJournalEntryCommand.builder()
                        .journalEntryId(original.getJournalEntryId())
                        .paymentId(original.getPaymentId())
                        .debitAccountId(original.getDebitAccountId())
                        .creditAccountId(original.getCreditAccountId())
                        .amount(original.getAmount().add(BigDecimal.ONE))
                        .currency(original.getCurrency())
                        .correlationId(original.getCorrelationId())
                        .idempotencyKey(UUID.randomUUID())
                        .build();

        assertThrows(
                IllegalStateException.class,
                () -> service.reverse(UUID.randomUUID(), mismatch)
        );
    }

    private JournalEntryRequestedCommand recordCommand() {
        return JournalEntryRequestedCommand.builder()
                .paymentId(UUID.randomUUID())
                .debitAccountId(UUID.randomUUID())
                .creditAccountId(UUID.randomUUID())
                .correlationId(UUID.randomUUID())
                .idempotencyKey(UUID.randomUUID())
                .amount(new BigDecimal("125.5000"))
                .currency(CurrencyType.USD)
                .build();
    }
}
