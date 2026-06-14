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
public class AccountDebitRequestedCommand {
    UUID paymentId;
    UUID accountId;
    UUID ownerUserId;

    BigDecimal amount;
    CurrencyType currency;

    UUID idempotencyKey;
    UUID correlationId;

    Instant requestedAt;
}
