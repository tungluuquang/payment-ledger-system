package org.vippro.command;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.vippro.util.CurrencyType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Value
@Builder
@Jacksonized
public class RecordReversalJournalEntryCommand {
    UUID journalEntryId;

    UUID paymentId;

    UUID reversalJournalEntryId;

    UUID debitAccountId;
    UUID creditAccountId;

    BigDecimal amount;
    CurrencyType currency;

    UUID correlationId;
    UUID idempotencyKey;

    Instant requestedAt;
}
