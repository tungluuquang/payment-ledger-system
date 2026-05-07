package org.vippro.event;

import lombok.Builder;
import lombok.Value;
import org.vippro.util.CurrencyType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class JournalEntryReversalRecorded {
    UUID eventId;
    UUID reversalJournalEntryId;
    UUID originalJournalEntryId;
    UUID paymentId;
    UUID correlationId;
    BigDecimal amount;
    CurrencyType currency;

    String reason;
    Instant occurredAt;
}
