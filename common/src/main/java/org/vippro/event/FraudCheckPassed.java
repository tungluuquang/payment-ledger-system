package org.vippro.event;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class FraudCheckPassed {
    UUID eventId;

    UUID paymentId;
    UUID accountId;

    UUID correlationId;

    Instant occurredAt;
}
