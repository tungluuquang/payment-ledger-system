package org.vippro.command;

import lombok.Builder;
import lombok.Value;
import org.vippro.util.CurrencyType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class ReverseAccountDebitCommand {
    UUID paymentId;
    UUID accountId;
    UUID originalTransactionId;

    BigDecimal amount;
    CurrencyType currency;

    UUID correlationId;
    UUID idempotencyKey;

    String reason;
    Instant requestedAt;
}
