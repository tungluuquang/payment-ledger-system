package org.vippro.command_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.vippro.command_service.model.EventRecord;

import java.util.UUID;

public interface EventRecordRepository extends JpaRepository<EventRecord, UUID> {
}
