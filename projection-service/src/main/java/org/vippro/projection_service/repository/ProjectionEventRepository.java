package org.vippro.projection_service.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.vippro.projection_service.model.ProjectionEvent;

import java.util.UUID;

public interface ProjectionEventRepository
        extends JpaRepository<ProjectionEvent, UUID> {

    Page<ProjectionEvent> findByPaymentId(
            UUID paymentId,
            Pageable pageable
    );
}
