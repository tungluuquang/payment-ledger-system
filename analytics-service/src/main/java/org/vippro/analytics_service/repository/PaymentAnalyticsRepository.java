package org.vippro.analytics_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.vippro.analytics_service.model.PaymentAnalytics;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PaymentAnalyticsRepository
        extends JpaRepository<PaymentAnalytics, UUID> {

    List<PaymentAnalytics> findByInitiatedAtGreaterThanEqual(
            Instant initiatedAt
    );
}
