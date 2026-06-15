package org.vippro.analytics_service.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record AnalyticsOverviewResponse(
        Instant windowStart,
        Instant windowEnd,
        long initiatedPayments,
        long completedPayments,
        long failedPayments,
        long cancelledPayments,
        long processingPayments,
        long consumedEvents,
        BigDecimal successRate,
        Long averageCompletionMillis,
        Map<String, BigDecimal> initiatedVolumeByCurrency,
        List<TrendPoint> trend,
        List<FailureBreakdown> failureBreakdown,
        List<RecentFailure> recentFailures,
        Instant generatedAt
) {
}
