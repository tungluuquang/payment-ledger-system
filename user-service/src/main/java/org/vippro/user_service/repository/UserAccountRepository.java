package org.vippro.user_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.vippro.user_service.model.UserAccount;

import java.util.Optional;
import java.util.UUID;

public interface UserAccountRepository
        extends JpaRepository<UserAccount, UUID> {

    Optional<UserAccount> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndUserIdNot(
            String email,
            UUID userId
    );
}
