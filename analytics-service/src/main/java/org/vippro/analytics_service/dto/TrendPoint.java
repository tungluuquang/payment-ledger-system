package org.vippro.analytics_service.dto;

import java.time.Instant;

public record TrendPoint(
        Instant bucketStart,
        long initiated,
        long completed,
        long failed,
        long cancelled
) {
}
