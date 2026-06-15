package org.vippro.audit_service.service;

import java.time.Instant;
import java.util.UUID;

record AuditEventMetadata(
        UUID eventId,
        UUID paymentId,
        UUID correlationId,
        Instant occurredAt
) {
}
