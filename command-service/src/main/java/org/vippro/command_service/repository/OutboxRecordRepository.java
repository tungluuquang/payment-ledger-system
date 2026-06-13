package org.vippro.command_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.vippro.command_service.model.OutboxRecord;

import java.util.UUID;

public interface OutboxRecordRepository extends JpaRepository<OutboxRecord, UUID> {
}
