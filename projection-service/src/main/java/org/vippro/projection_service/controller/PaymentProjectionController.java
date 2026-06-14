package org.vippro.projection_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import org.vippro.projection_service.dto.PaymentProjectionResponse;
import org.vippro.projection_service.dto.ProjectionEventResponse;
import org.vippro.projection_service.model.PaymentViewStatus;
import org.vippro.projection_service.service.PaymentQueryService;

import java.util.UUID;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentProjectionController {

    private final PaymentQueryService paymentQueryService;

    @GetMapping("/{paymentId}")
    public PaymentProjectionResponse find(
            @PathVariable UUID paymentId
    ) {
        return paymentQueryService.find(paymentId);
    }

    @GetMapping
    public Page<PaymentProjectionResponse> search(
            @RequestParam(required = false) PaymentViewStatus status,
            @RequestParam(required = false) UUID accountId,
            @RequestParam(required = false) UUID correlationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return paymentQueryService.search(
                status,
                accountId,
                correlationId,
                page,
                size
        );
    }

    @GetMapping("/{paymentId}/events")
    public Page<ProjectionEventResponse> timeline(
            @PathVariable UUID paymentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return paymentQueryService.timeline(paymentId, page, size);
    }
}
