package org.vippro.analytics_service.dto;

public record FailureBreakdown(
        String stage,
        String errorCode,
        long count
) {
}
