package org.vippro.analytics_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vippro.analytics_service.dto.*;
import org.vippro.analytics_service.model.AnalyticsEvent;
import org.vippro.analytics_service.model.AnalyticsPaymentStatus;
import org.vippro.analytics_service.model.PaymentAnalytics;
import org.vippro.analytics_service.repository.AnalyticsEventRepository;
import org.vippro.analytics_service.repository.PaymentAnalyticsRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsQueryService {

    private final PaymentAnalyticsRepository paymentRepository;
    private final AnalyticsEventRepository eventRepository;

    @Transactional(readOnly = true)
    public AnalyticsOverviewResponse overview(int requestedHours) {
        int hours = Math.min(Math.max(requestedHours, 1), 24 * 30);
        Instant end = Instant.now();
        Instant start = end.minus(hours, ChronoUnit.HOURS);
        List<PaymentAnalytics> payments =
                paymentRepository.findByInitiatedAtGreaterThanEqual(start);
        List<AnalyticsEvent> failures =
                eventRepository.findByEventCategoryAndOccurredAtGreaterThanEqual(
                        "FAILURE",
                        start,
                        PageRequest.of(
                                0,
                                100,
                                Sort.by(Sort.Direction.DESC, "occurredAt")
                        )
                );

        long completed = countStatus(
                payments,
                AnalyticsPaymentStatus.COMPLETED
        );
        long failed = countStatus(payments, AnalyticsPaymentStatus.FAILED);
        long cancelled = countStatus(
                payments,
                AnalyticsPaymentStatus.CANCELLED
        );
        long processing = payments.stream()
                .filter(payment ->
                        payment.getStatus()
                                == AnalyticsPaymentStatus.INITIATED
                                || payment.getStatus()
                                == AnalyticsPaymentStatus.PROCESSING)
                .count();
        long terminal = completed + failed + cancelled;

        return new AnalyticsOverviewResponse(
                start,
                end,
                payments.size(),
                completed,
                failed,
                cancelled,
                processing,
                eventRepository.countByOccurredAtGreaterThanEqual(start),
                terminal == 0
                        ? BigDecimal.ZERO
                        : BigDecimal.valueOf(completed)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(
                                BigDecimal.valueOf(terminal),
                                2,
                                RoundingMode.HALF_UP
                        ),
                averageCompletionMillis(payments),
                volumeByCurrency(payments),
                trend(payments, start, end, hours),
                failureBreakdown(failures),
                failures.stream()
                        .limit(12)
                        .map(this::recentFailure)
                        .toList(),
                Instant.now()
        );
    }

    private long countStatus(
            List<PaymentAnalytics> payments,
            AnalyticsPaymentStatus status
    ) {
        return payments.stream()
                .filter(payment -> payment.getStatus() == status)
                .count();
    }

    private Long averageCompletionMillis(
            List<PaymentAnalytics> payments
    ) {
        OptionalDouble average = payments.stream()
                .filter(payment -> payment.getInitiatedAt() != null)
                .filter(payment -> payment.getCompletedAt() != null)
                .mapToLong(payment -> Duration.between(
                        payment.getInitiatedAt(),
                        payment.getCompletedAt()
                ).toMillis())
                .filter(duration -> duration >= 0)
                .average();
        return average.isPresent() ? Math.round(average.getAsDouble()) : null;
    }

    private Map<String, BigDecimal> volumeByCurrency(
            List<PaymentAnalytics> payments
    ) {
        return payments.stream()
                .filter(payment -> payment.getCurrency() != null)
                .filter(payment -> payment.getAmount() != null)
                .collect(Collectors.groupingBy(
                        PaymentAnalytics::getCurrency,
                        TreeMap::new,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                PaymentAnalytics::getAmount,
                                BigDecimal::add
                        )
                ));
    }

    private List<TrendPoint> trend(
            List<PaymentAnalytics> payments,
            Instant start,
            Instant end,
            int hours
    ) {
        boolean daily = hours > 72;
        Function<Instant, Instant> bucket = instant ->
                bucket(instant, daily);
        Map<Instant, MutableTrend> values = new TreeMap<>();
        Instant cursor = bucket.apply(start);
        Instant last = bucket.apply(end);
        while (!cursor.isAfter(last)) {
            values.put(cursor, new MutableTrend());
            cursor = daily
                    ? cursor.plus(1, ChronoUnit.DAYS)
                    : cursor.plus(1, ChronoUnit.HOURS);
        }

        for (PaymentAnalytics payment : payments) {
            increment(values, payment.getInitiatedAt(), bucket,
                    trend -> trend.initiated++);
            increment(values, payment.getCompletedAt(), bucket,
                    trend -> trend.completed++);
            increment(values, payment.getFailedAt(), bucket,
                    trend -> trend.failed++);
            increment(values, payment.getCancelledAt(), bucket,
                    trend -> trend.cancelled++);
        }
        return values.entrySet().stream()
                .map(entry -> new TrendPoint(
                        entry.getKey(),
                        entry.getValue().initiated,
                        entry.getValue().completed,
                        entry.getValue().failed,
                        entry.getValue().cancelled
                ))
                .toList();
    }

    private void increment(
            Map<Instant, MutableTrend> values,
            Instant value,
            Function<Instant, Instant> bucket,
            java.util.function.Consumer<MutableTrend> increment
    ) {
        if (value == null) {
            return;
        }
        MutableTrend trend = values.get(bucket.apply(value));
        if (trend != null) {
            increment.accept(trend);
        }
    }

    private Instant bucket(Instant instant, boolean daily) {
        ZonedDateTime utc = instant.atZone(ZoneOffset.UTC);
        return daily
                ? utc.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant()
                : utc.truncatedTo(ChronoUnit.HOURS).toInstant();
    }

    private List<FailureBreakdown> failureBreakdown(
            List<AnalyticsEvent> failures
    ) {
        return failures.stream()
                .collect(Collectors.groupingBy(
                        event -> new FailureKey(
                                failureStage(event.getEventType()),
                                valueOrUnknown(event.getErrorCode())
                        ),
                        Collectors.counting()
                ))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<FailureKey, Long>comparingByValue()
                        .reversed())
                .limit(8)
                .map(entry -> new FailureBreakdown(
                        entry.getKey().stage(),
                        entry.getKey().errorCode(),
                        entry.getValue()
                ))
                .toList();
    }

    private RecentFailure recentFailure(AnalyticsEvent event) {
        return new RecentFailure(
                event.getEventId(),
                event.getPaymentId(),
                event.getCorrelationId(),
                event.getEventType(),
                event.getErrorCode(),
                event.getReason(),
                event.getOccurredAt()
        );
    }

    private String failureStage(String eventType) {
        if (eventType.startsWith("Fraud")) {
            return "FRAUD";
        }
        if (eventType.startsWith("AccountDebit")) {
            return "DEBIT";
        }
        if (eventType.startsWith("AccountCredit")) {
            return "CREDIT";
        }
        if (eventType.startsWith("Journal")) {
            return "LEDGER";
        }
        return "PAYMENT";
    }

    private String valueOrUnknown(String value) {
        return value == null || value.isBlank() ? "UNSPECIFIED" : value;
    }

    private record FailureKey(String stage, String errorCode) {
    }

    private static final class MutableTrend {
        private long initiated;
        private long completed;
        private long failed;
        private long cancelled;
    }
}
