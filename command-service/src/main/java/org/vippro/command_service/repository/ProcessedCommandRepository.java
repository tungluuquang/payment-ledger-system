package org.vippro.command_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.vippro.command_service.model.ProcessedCommand;

import java.time.Instant;
import java.util.UUID;

public interface ProcessedCommandRepository
        extends JpaRepository<ProcessedCommand, UUID> {

    @Modifying
    @Query(value = """
            INSERT INTO processed_commands (command_id, command_type, processed_at)
            VALUES (:commandId, :commandType, :processedAt)
            ON CONFLICT (command_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("commandId") UUID commandId,
            @Param("commandType") String commandType,
            @Param("processedAt") Instant processedAt
    );
}
