package org.vippro.ledger_service.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.vippro.command.JournalEntryRequestedCommand;
import org.vippro.command.RecordReversalJournalEntryCommand;
import org.vippro.ledger_service.service.LedgerCommandService;
import org.vippro.messaging.CommandEnvelope;

@Component
@RequiredArgsConstructor
public class LedgerCommandConsumer {

    private final ObjectMapper objectMapper;
    private final LedgerCommandService ledgerCommandService;

    @KafkaListener(
            topics = "ledger-commands",
            containerFactory = "ledgerCommandKafkaListenerContainerFactory"
    )
    public void consume(String message) throws JsonProcessingException {
        CommandEnvelope envelope = objectMapper.readValue(
                message,
                CommandEnvelope.class
        );
        validateEnvelope(envelope);

        switch (envelope.getCommandType()) {
            case "JournalEntryRequestedCommand" ->
                    ledgerCommandService.record(
                            envelope.getCommandId(),
                            objectMapper.treeToValue(
                                    envelope.getPayload(),
                                    JournalEntryRequestedCommand.class
                            )
                    );
            case "RecordReversalJournalEntryCommand" ->
                    ledgerCommandService.reverse(
                            envelope.getCommandId(),
                            objectMapper.treeToValue(
                                    envelope.getPayload(),
                                    RecordReversalJournalEntryCommand.class
                            )
                    );
            default -> throw new IllegalArgumentException(
                    "Unknown ledger command type: "
                            + envelope.getCommandType()
            );
        }
    }

    private void validateEnvelope(CommandEnvelope envelope) {
        if (envelope.getCommandId() == null
                || envelope.getCommandType() == null
                || envelope.getPayload() == null) {
            throw new IllegalArgumentException("Invalid command envelope");
        }
    }
}
