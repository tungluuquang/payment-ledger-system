package org.vippro.event;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class PaymentFailed {
    UUID eventId;
    UUID paymentId;

    UUID correlationId;

    String reason;
    String errorCode;

    Instant occurredAt;
}
