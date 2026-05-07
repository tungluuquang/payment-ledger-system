package org.vippro.command;

import lombok.Builder;
import lombok.Value;
import org.vippro.util.CurrencyType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class AccountDebitRequestedCommand {
    UUID paymentId;
    UUID accountId;

    BigDecimal amount;
    CurrencyType currency;

    UUID idempotencyKey;
    UUID correlationId;

    Instant requestedAt;
}
