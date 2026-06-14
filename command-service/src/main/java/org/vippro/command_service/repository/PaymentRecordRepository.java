package org.vippro.command_service.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.vippro.command_service.model.PaymentRecord;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRecordRepository
        extends JpaRepository<PaymentRecord, UUID> {

    Optional<PaymentRecord> findByIdempotencyKey(UUID idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PaymentRecord p WHERE p.paymentId = :paymentId")
    Optional<PaymentRecord> findByIdForUpdate(
            @Param("paymentId") UUID paymentId
    );
}
