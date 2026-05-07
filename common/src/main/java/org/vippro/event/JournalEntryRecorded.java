package org.vippro.event;

import lombok.Builder;
import lombok.Value;
import org.vippro.util.CurrencyType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class JournalEntryRecorded {
    UUID eventId;
    UUID journalEntryId;
    UUID paymentId;

    UUID debitAccountId;
    UUID creditAccountId;
    UUID correlationId;

    BigDecimal amount;
    CurrencyType currency;

    String description;
    long version;
    Instant occurredAt;

}
