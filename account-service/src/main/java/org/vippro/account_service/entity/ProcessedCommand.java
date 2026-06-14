package org.vippro.account_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processed_commands")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ProcessedCommand {

    @Id
    @Column(name = "command_id", nullable = false, updatable = false)
    private UUID commandId;

    @Column(name = "command_type", nullable = false, updatable = false)
    private String commandType;

    @Column(name = "processed_at", nullable = false, updatable = false)
    private Instant processedAt;
}
