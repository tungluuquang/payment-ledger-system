package org.vippro.event;

import lombok.Builder;
import lombok.Value;
import org.vippro.util.FailedStep;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class PaymentCancelled {
    UUID eventId;
    UUID paymentId;
    UUID correlationId;

    FailedStep failedStep;
    String reason;
    Instant occurredAt;
}
