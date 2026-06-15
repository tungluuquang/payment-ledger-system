package org.vippro.audit_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.vippro.audit_service.model.AuditEvent;

import java.util.UUID;

public interface AuditEventRepository
        extends JpaRepository<AuditEvent, UUID>,
        JpaSpecificationExecutor<AuditEvent> {
}
