package org.vippro.analytics_service.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.vippro.analytics_service.model.AnalyticsEvent;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AnalyticsEventRepository
        extends JpaRepository<AnalyticsEvent, UUID> {

    long countByOccurredAtGreaterThanEqual(Instant occurredAt);

    List<AnalyticsEvent> findByEventCategoryAndOccurredAtGreaterThanEqual(
            String eventCategory,
            Instant occurredAt,
            Pageable pageable
    );
}
