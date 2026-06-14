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
public class AccountDebitReversed {
    UUID eventId;
    UUID paymentId;

    UUID accountId;
    UUID originalTransactionId;
    UUID reversalTransactionId;

    BigDecimal amount;
    CurrencyType currency;

    UUID correlationId;
    Instant occurredAt;
}
