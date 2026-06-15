package org.vippro.audit_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vippro.audit_service.dto.AuditEventResponse;
import org.vippro.audit_service.model.AuditEvent;
import org.vippro.audit_service.repository.AuditEventRepository;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditQueryService {

    private final AuditEventRepository repository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public Page<AuditEventResponse> search(
            UUID paymentId,
            UUID correlationId,
            String eventType,
            String traceId,
            Instant from,
            Instant to,
            int page,
            int size
    ) {
        Specification<AuditEvent> specification =
                Specification.where(null);
        if (paymentId != null) {
            specification = specification.and(equal("paymentId", paymentId));
        }
        if (correlationId != null) {
            specification = specification.and(
                    equal("correlationId", correlationId)
            );
        }
        if (eventType != null && !eventType.isBlank()) {
            specification = specification.and(equal("eventType", eventType));
        }
        if (traceId != null && !traceId.isBlank()) {
            specification = specification.and(equal("traceId", traceId));
        }
        if (from != null) {
            specification = specification.and(
                    (root, query, builder) -> builder.greaterThanOrEqualTo(
                            root.get("occurredAt"),
                            from
                    )
            );
        }
        if (to != null) {
            specification = specification.and(
                    (root, query, builder) -> builder.lessThanOrEqualTo(
                            root.get("occurredAt"),
                            to
                    )
            );
        }

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 200),
                Sort.by(
                        Sort.Order.desc("occurredAt"),
                        Sort.Order.desc("recordedAt")
                )
        );
        return repository.findAll(specification, pageable)
                .map(event -> AuditEventResponse.from(event, objectMapper));
    }

    private <T> Specification<AuditEvent> equal(String field, T value) {
        return (root, query, builder) -> builder.equal(
                root.get(field),
                value
        );
    }
}
