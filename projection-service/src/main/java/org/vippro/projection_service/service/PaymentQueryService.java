package org.vippro.projection_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.vippro.projection_service.dto.PaymentProjectionResponse;
import org.vippro.projection_service.dto.ProjectionEventResponse;
import org.vippro.projection_service.model.PaymentProjection;
import org.vippro.projection_service.model.PaymentViewStatus;
import org.vippro.projection_service.repository.PaymentProjectionRepository;
import org.vippro.projection_service.repository.ProjectionEventRepository;

import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class PaymentQueryService {

    private final PaymentProjectionRepository projectionRepository;
    private final ProjectionEventRepository eventRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public PaymentProjectionResponse find(UUID paymentId) {
        return projectionRepository.findById(paymentId)
                .map(PaymentProjectionResponse::from)
                .orElseThrow(() -> new ResponseStatusException(
                        NOT_FOUND,
                        "Payment projection not found"
                ));
    }

    @Transactional(readOnly = true)
    public Page<PaymentProjectionResponse> search(
            PaymentViewStatus status,
            UUID accountId,
            UUID correlationId,
            int page,
            int size
    ) {
        Specification<PaymentProjection> specification =
                Specification.where(null);
        if (status != null) {
            specification = specification.and(
                    (root, query, builder) -> builder.equal(
                            root.get("paymentStatus"),
                            status
                    )
            );
        }
        if (accountId != null) {
            specification = specification.and(
                    (root, query, builder) -> builder.or(
                            builder.equal(
                                    root.get("sourceAccountId"),
                                    accountId
                            ),
                            builder.equal(
                                    root.get("destinationAccountId"),
                                    accountId
                            )
                    )
            );
        }
        if (correlationId != null) {
            specification = specification.and(
                    (root, query, builder) -> builder.equal(
                            root.get("correlationId"),
                            correlationId
                    )
            );
        }

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "updatedAt")
        );
        return projectionRepository.findAll(specification, pageable)
                .map(PaymentProjectionResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<ProjectionEventResponse> timeline(
            UUID paymentId,
            int page,
            int size
    ) {
        if (!projectionRepository.existsById(paymentId)) {
            throw new ResponseStatusException(
                    NOT_FOUND,
                    "Payment projection not found"
            );
        }
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by(
                        Sort.Order.asc("occurredAt"),
                        Sort.Order.asc("processedAt")
                )
        );
        return eventRepository.findByPaymentId(paymentId, pageable)
                .map(event -> ProjectionEventResponse.from(
                        event,
                        objectMapper
                ));
    }
}
