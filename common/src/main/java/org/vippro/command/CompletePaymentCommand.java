package org.vippro.command;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class CompletePaymentCommand {
    UUID paymentId;

    UUID correlationId;
    UUID idempotencyKey;

    Instant completedAt;
}
