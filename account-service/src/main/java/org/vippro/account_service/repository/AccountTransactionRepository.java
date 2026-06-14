package org.vippro.account_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.vippro.account_service.entity.AccountTransaction;

import java.util.Optional;
import java.util.UUID;

public interface AccountTransactionRepository
        extends JpaRepository<AccountTransaction, UUID> {

    Optional<AccountTransaction> findByOriginalTransactionId(
            UUID originalTransactionId
    );

    Optional<AccountTransaction> findByIdempotencyKey(UUID idempotencyKey);
}
