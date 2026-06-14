package org.vippro.command_service.model;

import jakarta.persistence.*;
import lombok.*;

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
