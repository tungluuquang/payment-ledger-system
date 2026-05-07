package org.vippro.event;

import lombok.Builder;
import lombok.Value;
import org.vippro.util.CurrencyType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class AccountDebited {
    UUID paymentId;
    UUID accountId;
    BigDecimal amount;
    CurrencyType currency;

    UUID eventId;
    UUID transactionId;
    UUID correlationId;
    Instant occurredAt;
}
