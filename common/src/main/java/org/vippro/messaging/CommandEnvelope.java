package org.vippro.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
@Jacksonized
public class CommandEnvelope {
    UUID commandId;
    UUID sagaId;
    String commandType;
    JsonNode payload;
    Instant createdAt;
}
