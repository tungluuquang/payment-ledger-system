package org.vippro.command;

import lombok.Value;
import lombok.Builder;
import org.vippro.util.CurrencyType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class FraudCheckRequestedCommand {
    UUID paymentId;
    UUID accountId;

    BigDecimal amount;
    CurrencyType currency;

    UUID idempotencyKey;
    UUID correlationId;

    Instant requestedAt;
}
