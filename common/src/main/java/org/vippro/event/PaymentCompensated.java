package org.vippro.event;

import lombok.Builder;
import lombok.Value;
import org.vippro.util.CompensationType;
import org.vippro.util.FailedStep;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class PaymentCompensated {
    UUID eventId;
    UUID paymentId;
    UUID compensationId;
    UUID correlationId;

    FailedStep failedStep;
    CompensationType compensationType;
    Instant occurredAt;
}
