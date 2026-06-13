package org.vippro.saga_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.vippro.saga_service.model.PaymentState;
import org.vippro.saga_service.model.SagaState;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SagaStateRepository extends JpaRepository<SagaState, UUID> {
    Optional<SagaState> findByPaymentId(UUID paymentId);

    List<SagaState> findByPaymentState(PaymentState paymentState);

    Optional<SagaState> findByPaymentIdAndPaymentState(
            UUID paymentId,
            PaymentState paymentState
    );
}
