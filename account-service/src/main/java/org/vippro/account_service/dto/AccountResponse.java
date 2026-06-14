package org.vippro.account_service.dto;

import org.vippro.account_service.entity.Account;
import org.vippro.account_service.enums.AccountStatus;
import org.vippro.util.CurrencyType;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountResponse(
        UUID accountId,
        BigDecimal balance,
        CurrencyType currency,
        AccountStatus status
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getAccountId(),
                account.getBalance(),
                account.getCurrency(),
                account.getStatus()
        );
    }
}
