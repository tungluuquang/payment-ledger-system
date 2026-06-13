package org.vippro.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class CommandEnvelope {
    UUID commandId;
    UUID sagaId;
    String commandType;
    JsonNode payload;
    Instant createdAt;
}