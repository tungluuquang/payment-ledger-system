package org.vippro.reconciliation_service.model;

import java.util.UUID;

public record ReconciliationKey(
        UUID accountId,
        String currency,
        UUID correlationId
) {
}
