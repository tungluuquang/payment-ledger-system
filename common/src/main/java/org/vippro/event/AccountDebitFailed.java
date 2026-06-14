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
public class AccountDebitFailed {
    UUID eventId;
    UUID correlationId;

    UUID paymentId;
    UUID accountId;

    BigDecimal amount;
    CurrencyType currency;

    String reason;
    String errorCode;

    Instant occurredAt;
}
