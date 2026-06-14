package org.vippro.event;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
@Jacksonized
public class FraudCheckPassed {
    UUID eventId;

    UUID paymentId;
    UUID accountId;

    UUID correlationId;

    Instant occurredAt;
}
