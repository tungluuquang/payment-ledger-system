package org.vippro.account_service.dto;

import org.vippro.util.CurrencyType;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateAccountRequest(
        UUID accountId,
        BigDecimal initialBalance,
        CurrencyType currency
) {
}
