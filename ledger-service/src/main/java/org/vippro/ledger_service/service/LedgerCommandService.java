package org.vippro.ledger_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vippro.command.JournalEntryRequestedCommand;
import org.vippro.command.RecordReversalJournalEntryCommand;
import org.vippro.event.JournalEntryFailed;
import org.vippro.event.JournalEntryRecorded;
import org.vippro.event.JournalEntryReversalRecorded;
import org.vippro.ledger_service.model.*;
import org.vippro.ledger_service.repository.JournalEntryRepository;
import org.vippro.ledger_service.repository.LedgerPostingRepository;
import org.vippro.ledger_service.repository.ProcessedCommandStore;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LedgerCommandService {

    private final JournalEntryRepository journalEntryRepository;
    private final LedgerPostingRepository postingRepository;
    private final ProcessedCommandStore processedCommandStore;
    private final EventOutboxService eventOutboxService;

    @Transactional
    public void record(
            UUID commandId,
            JournalEntryRequestedCommand command
    ) {
        requireCommandId(commandId);
        validateRecordStructure(command);

        if (!tryProcess(
                commandId,
                JournalEntryRequestedCommand.class.getSimpleName()
        )) {
            return;
        }

        UUID journalEntryId = UUID.randomUUID();
        if (command.getAmount() == null
                || command.getAmount().signum() <= 0) {
            publishFailure(
                    journalEntryId,
                    command,
                    "INVALID_AMOUNT",
                    "Journal amount must be positive"
            );
            return;
        }
        if (command.getDebitAccountId().equals(command.getCreditAccountId())) {
            publishFailure(
                    journalEntryId,
                    command,
                    "SAME_ACCOUNT",
                    "Debit and credit accounts must differ"
            );
            return;
        }

        JournalEntry existing = journalEntryRepository
                .findByIdempotencyKey(command.getIdempotencyKey())
                .orElse(null);
        if (existing != null) {
            validateExistingRecord(existing, command);
            return;
        }

        Instant occurredAt = Instant.now();
        JournalEntry entry = JournalEntry.builder()
                .journalEntryId(journalEntryId)
                .paymentId(command.getPaymentId())
                .correlationId(command.getCorrelationId())
                .idempotencyKey(command.getIdempotencyKey())
                .entryType(JournalEntryType.PAYMENT)
                .debitAccountId(command.getDebitAccountId())
                .creditAccountId(command.getCreditAccountId())
                .amount(command.getAmount())
                .currency(command.getCurrency())
                .description("Payment journal entry")
                .version(1)
                .createdAt(occurredAt)
                .build();

        journalEntryRepository.save(entry);
        saveBalancedPostings(entry);

        UUID eventId = UUID.randomUUID();
        JournalEntryRecorded event = JournalEntryRecorded.builder()
                .eventId(eventId)
                .journalEntryId(journalEntryId)
                .paymentId(command.getPaymentId())
                .debitAccountId(command.getDebitAccountId())
                .creditAccountId(command.getCreditAccountId())
                .correlationId(command.getCorrelationId())
                .amount(command.getAmount())
                .currency(command.getCurrency())
                .description(entry.getDescription())
                .version(entry.getVersion())
                .occurredAt(occurredAt)
                .build();
        eventOutboxService.save(eventId, journalEntryId, event);
    }

    @Transactional
    public void reverse(
            UUID commandId,
            RecordReversalJournalEntryCommand command
    ) {
        requireCommandId(commandId);
        validateReversalStructure(command);

        if (!tryProcess(
                commandId,
                RecordReversalJournalEntryCommand.class.getSimpleName()
        )) {
            return;
        }

        JournalEntry existing = journalEntryRepository
                .findByIdempotencyKey(command.getIdempotencyKey())
                .orElse(null);
        if (existing != null) {
            validateExistingReversal(existing, command);
            return;
        }

        JournalEntry original = journalEntryRepository
                .findByIdForUpdate(command.getJournalEntryId())
                .orElseThrow(() -> new IllegalStateException(
                        "Original journal entry not found"
                ));
        validateOriginal(original, command);

        JournalEntry priorReversal = journalEntryRepository
                .findByOriginalJournalEntryId(original.getJournalEntryId())
                .orElse(null);
        if (priorReversal != null) {
            throw new IllegalStateException(
                    "Journal entry has already been reversed"
            );
        }

        UUID reversalId = command.getReversalJournalEntryId() == null
                ? UUID.randomUUID()
                : command.getReversalJournalEntryId();
        Instant occurredAt = Instant.now();
        JournalEntry reversal = JournalEntry.builder()
                .journalEntryId(reversalId)
                .paymentId(command.getPaymentId())
                .correlationId(command.getCorrelationId())
                .idempotencyKey(command.getIdempotencyKey())
                .entryType(JournalEntryType.REVERSAL)
                .originalJournalEntryId(original.getJournalEntryId())
                .debitAccountId(original.getCreditAccountId())
                .creditAccountId(original.getDebitAccountId())
                .amount(original.getAmount())
                .currency(original.getCurrency())
                .description("Reversal of " + original.getJournalEntryId())
                .version(1)
                .createdAt(occurredAt)
                .build();

        journalEntryRepository.save(reversal);
        saveBalancedPostings(reversal);

        UUID eventId = UUID.randomUUID();
        JournalEntryReversalRecorded event =
                JournalEntryReversalRecorded.builder()
                        .eventId(eventId)
                        .reversalJournalEntryId(reversalId)
                        .originalJournalEntryId(
                                original.getJournalEntryId()
                        )
                        .paymentId(command.getPaymentId())
                        .correlationId(command.getCorrelationId())
                        .amount(original.getAmount())
                        .currency(original.getCurrency())
                        .reason(reversal.getDescription())
                        .occurredAt(occurredAt)
                        .build();
        eventOutboxService.save(eventId, reversalId, event);
    }

    private void saveBalancedPostings(JournalEntry entry) {
        LedgerPosting debit = LedgerPosting.builder()
                .postingId(UUID.randomUUID())
                .journalEntryId(entry.getJournalEntryId())
                .accountId(entry.getDebitAccountId())
                .side(PostingSide.DEBIT)
                .amount(entry.getAmount())
                .currency(entry.getCurrency())
                .createdAt(entry.getCreatedAt())
                .build();
        LedgerPosting credit = LedgerPosting.builder()
                .postingId(UUID.randomUUID())
                .journalEntryId(entry.getJournalEntryId())
                .accountId(entry.getCreditAccountId())
                .side(PostingSide.CREDIT)
                .amount(entry.getAmount())
                .currency(entry.getCurrency())
                .createdAt(entry.getCreatedAt())
                .build();
        postingRepository.saveAll(List.of(debit, credit));
    }

    private void publishFailure(
            UUID journalEntryId,
            JournalEntryRequestedCommand command,
            String errorCode,
            String reason
    ) {
        UUID eventId = UUID.randomUUID();
        JournalEntryFailed event = JournalEntryFailed.builder()
                .eventId(eventId)
                .journalEntryId(journalEntryId)
                .paymentId(command.getPaymentId())
                .correlationId(command.getCorrelationId())
                .debitAccountId(command.getDebitAccountId())
                .creditAccountId(command.getCreditAccountId())
                .errorCode(errorCode)
                .reason(reason)
                .occurredAt(Instant.now())
                .build();
        eventOutboxService.save(eventId, journalEntryId, event);
    }

    private boolean tryProcess(UUID commandId, String commandType) {
        return processedCommandStore.insertIfAbsent(
                commandId,
                commandType,
                Instant.now()
        );
    }

    private void validateRecordStructure(
            JournalEntryRequestedCommand command
    ) {
        if (command == null
                || command.getPaymentId() == null
                || command.getDebitAccountId() == null
                || command.getCreditAccountId() == null
                || command.getCorrelationId() == null
                || command.getIdempotencyKey() == null
                || command.getCurrency() == null) {
            throw new IllegalArgumentException(
                    "Invalid JournalEntryRequestedCommand"
            );
        }
    }

    private void validateReversalStructure(
            RecordReversalJournalEntryCommand command
    ) {
        if (command == null
                || command.getJournalEntryId() == null
                || command.getPaymentId() == null
                || command.getDebitAccountId() == null
                || command.getCreditAccountId() == null
                || command.getAmount() == null
                || command.getAmount().signum() <= 0
                || command.getCurrency() == null
                || command.getCorrelationId() == null
                || command.getIdempotencyKey() == null) {
            throw new IllegalArgumentException(
                    "Invalid RecordReversalJournalEntryCommand"
            );
        }
    }

    private void validateExistingRecord(
            JournalEntry existing,
            JournalEntryRequestedCommand command
    ) {
        if (existing.getEntryType() != JournalEntryType.PAYMENT
                || !Objects.equals(existing.getPaymentId(), command.getPaymentId())
                || !Objects.equals(existing.getDebitAccountId(), command.getDebitAccountId())
                || !Objects.equals(existing.getCreditAccountId(), command.getCreditAccountId())
                || existing.getAmount().compareTo(command.getAmount()) != 0
                || existing.getCurrency() != command.getCurrency()
                || !Objects.equals(existing.getCorrelationId(), command.getCorrelationId())) {
            throw new IllegalStateException(
                    "Idempotency key was used for another journal entry"
            );
        }
    }

    private void validateOriginal(
            JournalEntry original,
            RecordReversalJournalEntryCommand command
    ) {
        if (original.getEntryType() != JournalEntryType.PAYMENT
                || !Objects.equals(original.getPaymentId(), command.getPaymentId())
                || !Objects.equals(original.getDebitAccountId(), command.getDebitAccountId())
                || !Objects.equals(original.getCreditAccountId(), command.getCreditAccountId())
                || original.getAmount().compareTo(command.getAmount()) != 0
                || original.getCurrency() != command.getCurrency()
                || !Objects.equals(original.getCorrelationId(), command.getCorrelationId())) {
            throw new IllegalStateException(
                    "Reversal command does not match original journal entry"
            );
        }
    }

    private void validateExistingReversal(
            JournalEntry existing,
            RecordReversalJournalEntryCommand command
    ) {
        if (existing.getEntryType() != JournalEntryType.REVERSAL
                || !Objects.equals(existing.getOriginalJournalEntryId(), command.getJournalEntryId())
                || (command.getReversalJournalEntryId() != null
                    && !Objects.equals(existing.getJournalEntryId(), command.getReversalJournalEntryId()))
                || !Objects.equals(existing.getPaymentId(), command.getPaymentId())
                || !Objects.equals(existing.getDebitAccountId(), command.getCreditAccountId())
                || !Objects.equals(existing.getCreditAccountId(), command.getDebitAccountId())
                || existing.getAmount().compareTo(command.getAmount()) != 0
                || existing.getCurrency() != command.getCurrency()
                || !Objects.equals(existing.getCorrelationId(), command.getCorrelationId())) {
            throw new IllegalStateException(
                    "Idempotency key was used for another reversal"
            );
        }
    }

    private void requireCommandId(UUID commandId) {
        if (commandId == null) {
            throw new IllegalArgumentException("commandId is required");
        }
    }
}
