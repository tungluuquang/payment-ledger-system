package org.vippro.command;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.vippro.util.CurrencyType;

import java.math.BigDecimal;
import java.util.UUID;

@Value
@Builder
@Jacksonized
public class InitiatePaymentCommand {
    UUID sourceAccountId;
    UUID destinationAccountId;
    UUID correlationId;

    BigDecimal amount;
    CurrencyType currency;

    UUID idempotencyKey;
    String description;
}
