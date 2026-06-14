package org.vippro.event;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
@Jacksonized
public class PaymentFailed {
    UUID eventId;
    UUID paymentId;

    UUID correlationId;

    String reason;
    String errorCode;

    Instant occurredAt;
}
