package org.vippro.audit_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.vippro.audit_service.dto.AuditEventResponse;
import org.vippro.audit_service.service.AuditQueryService;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/audit/events")
@RequiredArgsConstructor
public class AuditController {

    private final AuditQueryService queryService;

    @GetMapping
    public Page<AuditEventResponse> search(
            @RequestParam(required = false) UUID paymentId,
            @RequestParam(required = false) UUID correlationId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String traceId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return queryService.search(
                paymentId,
                correlationId,
                eventType,
                traceId,
                from,
                to,
                page,
                size
        );
    }
}
