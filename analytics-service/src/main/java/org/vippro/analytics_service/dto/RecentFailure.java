package org.vippro.analytics_service.dto;

import java.time.Instant;
import java.util.UUID;

public record RecentFailure(
        UUID eventId,
        UUID paymentId,
        UUID correlationId,
        String eventType,
        String errorCode,
        String reason,
        Instant occurredAt
) {
}
