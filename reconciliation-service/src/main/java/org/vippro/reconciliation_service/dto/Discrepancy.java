package org.vippro.reconciliation_service.dto;

import org.vippro.reconciliation_service.model.ReconciliationKey;

import java.math.BigDecimal;

public record Discrepancy(
        String code,
        String message,
        ReconciliationKey key,
        BigDecimal accountNetAmount,
        BigDecimal ledgerNetAmount
) {
}
