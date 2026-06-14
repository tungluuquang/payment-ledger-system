package org.vippro.projection_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.vippro.projection_service.model.PaymentProjection;

import java.util.UUID;

public interface PaymentProjectionRepository
        extends JpaRepository<PaymentProjection, UUID>,
        JpaSpecificationExecutor<PaymentProjection> {
}
