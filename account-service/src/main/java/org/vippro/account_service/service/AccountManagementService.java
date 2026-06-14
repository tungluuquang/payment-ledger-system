package org.vippro.account_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vippro.account_service.entity.Account;
import org.vippro.account_service.enums.AccountStatus;
import org.vippro.account_service.repository.AccountRepository;
import org.vippro.util.CurrencyType;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountManagementService {

    private final AccountRepository accountRepository;

    @Transactional
    public Account create(
            UUID ownerUserId,
            UUID accountId,
            BigDecimal initialBalance,
            CurrencyType currency
    ) {
        if (ownerUserId == null
                || initialBalance == null
                || initialBalance.signum() < 0
                || currency == null) {
            throw new IllegalArgumentException(
                    "Initial balance must be non-negative and currency is required"
            );
        }

        UUID id = accountId == null ? UUID.randomUUID() : accountId;
        if (accountRepository.existsById(id)) {
            throw new IllegalStateException("Account already exists: " + id);
        }

        return accountRepository.save(
                Account.builder()
                        .accountId(id)
                        .ownerUserId(ownerUserId)
                        .balance(initialBalance)
                        .currency(currency)
                        .status(AccountStatus.ACTIVE)
                        .build()
        );
    }

    @Transactional(readOnly = true)
    public Account find(UUID accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalStateException(
                        "Account not found: " + accountId
                ));
    }

    @Transactional(readOnly = true)
    public Account findOwned(UUID accountId, UUID requesterUserId, boolean admin) {
        Account account = find(accountId);
        if (!admin && !account.getOwnerUserId().equals(requesterUserId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Account does not belong to authenticated user"
            );
        }
        return account;
    }
}
