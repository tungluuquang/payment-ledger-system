package org.vippro.event;

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
public class PaymentInitiated {
    UUID eventId;
    UUID paymentId;

    UUID sourceAccountId;
    UUID destinationAccountId;

    BigDecimal amount;
    CurrencyType currency;

    UUID correlationId;
    UUID idempotencyKey;
    Instant occurredAt;
}
