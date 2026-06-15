package org.vippro.reconciliation_service.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record BatchReconciliationRequest(
        @NotEmpty
        @Size(max = 100)
        List<UUID> paymentIds
) {
}
