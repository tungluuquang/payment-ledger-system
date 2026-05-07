package org.vippro.command;

import lombok.Builder;
import lombok.Value;
import org.vippro.util.CurrencyType;

import java.math.BigDecimal;
import java.util.UUID;

@Value
@Builder
public class InitiatePaymentCommand {
    UUID customerId;
    UUID merchantId;
    UUID correlationId;

    BigDecimal amount;
    CurrencyType currency;

    UUID idempotencyKey;
    String description;
}
