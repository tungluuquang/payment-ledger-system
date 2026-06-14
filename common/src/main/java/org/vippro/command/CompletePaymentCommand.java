package org.vippro.command;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
@Jacksonized
public class CompletePaymentCommand {
    UUID paymentId;

    UUID correlationId;
    Instant completedAt;
}
