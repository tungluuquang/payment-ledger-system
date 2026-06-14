package org.vippro.projection_service.service;

import java.time.Instant;
import java.util.UUID;

public record EventMetadata(
        UUID eventId,
        UUID paymentId,
        UUID correlationId,
        Instant occurredAt
) {
}
