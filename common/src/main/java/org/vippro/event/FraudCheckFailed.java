package org.vippro.event;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
@Jacksonized
public class FraudCheckFailed {
    UUID eventId;

    UUID paymentId;
    UUID accountId;

    String reason;

    UUID correlationId;

    Instant occurredAt;
}
