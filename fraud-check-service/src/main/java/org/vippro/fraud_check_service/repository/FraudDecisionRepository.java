package org.vippro.fraud_check_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.vippro.fraud_check_service.model.FraudDecision;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface FraudDecisionRepository
        extends JpaRepository<FraudDecision, UUID> {

    Optional<FraudDecision> findByIdempotencyKey(UUID idempotencyKey);

    Optional<FraudDecision> findByPaymentId(UUID paymentId);

    long countByAccountIdAndCreatedAtGreaterThanEqual(
            UUID accountId,
            Instant threshold
    );
}
