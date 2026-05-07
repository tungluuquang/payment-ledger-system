package org.vippro.event;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class AccountDebitFailed {
    UUID eventId;
    UUID correlationId;

    UUID paymentId;
    UUID accountId;

    BigDecimal amount;
    String currency;

    String reason;
    String errorCode;

    Instant occurredAt;
}
