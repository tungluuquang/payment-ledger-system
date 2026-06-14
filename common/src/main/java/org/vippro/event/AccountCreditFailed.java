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
public class AccountCreditFailed {
    UUID eventId;
    UUID paymentId;
    UUID accountId;
    BigDecimal amount;
    CurrencyType currency;
    UUID correlationId;
    String errorCode;
    String reason;
    Instant occurredAt;
}
