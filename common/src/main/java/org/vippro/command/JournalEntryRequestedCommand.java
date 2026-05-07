package org.vippro.command;

import lombok.Builder;
import lombok.Value;
import org.vippro.util.CurrencyType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class JournalEntryRequestedCommand {
    UUID paymentId;

    UUID debitAccountId;
    UUID creditAccountId;
    UUID correlationId;
    UUID idempotencyKey;

    BigDecimal amount;
    CurrencyType currency;

    Instant requestedAt;
}
