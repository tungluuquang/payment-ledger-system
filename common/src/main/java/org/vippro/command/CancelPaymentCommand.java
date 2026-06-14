package org.vippro.command;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
@Jacksonized
public class CancelPaymentCommand {
    UUID paymentId;

    UUID correlationId;
    Instant cancelledAt;
    String reason;
}
