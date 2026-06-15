package org.vippro.reconciliation_service.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.vippro.reconciliation_service.dto.BatchReconciliationReport;
import org.vippro.reconciliation_service.dto.BatchReconciliationRequest;
import org.vippro.reconciliation_service.dto.PaymentReconciliationReport;
import org.vippro.reconciliation_service.service.ReconciliationService;

import java.util.UUID;

@RestController
@RequestMapping("/reconciliations")
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    public ReconciliationController(
            ReconciliationService reconciliationService
    ) {
        this.reconciliationService = reconciliationService;
    }

    @GetMapping("/payments/{paymentId}")
    public PaymentReconciliationReport reconcilePayment(
            @PathVariable UUID paymentId
    ) {
        return reconciliationService.reconcilePayment(paymentId);
    }

    @PostMapping("/payments")
    public BatchReconciliationReport reconcilePayments(
            @Valid @RequestBody BatchReconciliationRequest request
    ) {
        return reconciliationService.reconcilePayments(request.paymentIds());
    }
}
