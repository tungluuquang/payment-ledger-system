package org.vippro.analytics_service.service;

import java.time.Instant;
import java.util.UUID;

public record AnalyticsEventMetadata(
        UUID eventId,
        UUID paymentId,
        UUID correlationId,
        Instant occurredAt,
        String category,
        String failureStage,
        String errorCode,
        String reason
) {
}
