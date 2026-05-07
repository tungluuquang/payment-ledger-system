package org.vippro.event;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class JournalEntryFailed {
    UUID eventId;
    UUID journalEntryId;
    UUID paymentId;

    UUID correlationId;
    UUID debitAccountId;
    UUID creditAccountId;

    String reason;
    String errorCode;

    Instant occurredAt;
}
