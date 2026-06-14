package org.vippro.fraud_check_service.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.vippro.command.FraudCheckRequestedCommand;
import org.vippro.fraud_check_service.service.FraudCheckService;
import org.vippro.messaging.CommandEnvelope;

@Component
@RequiredArgsConstructor
public class FraudCommandConsumer {

    private final ObjectMapper objectMapper;
    private final FraudCheckService fraudCheckService;

    @KafkaListener(
            topics = "fraud-check-commands",
            containerFactory = "fraudCommandKafkaListenerContainerFactory"
    )
    public void consume(String message) throws JsonProcessingException {
        CommandEnvelope envelope = objectMapper.readValue(
                message,
                CommandEnvelope.class
        );
        if (envelope.getCommandId() == null
                || envelope.getCommandType() == null
                || envelope.getPayload() == null) {
            throw new IllegalArgumentException("Invalid command envelope");
        }
        if (!FraudCheckRequestedCommand.class.getSimpleName()
                .equals(envelope.getCommandType())) {
            throw new IllegalArgumentException(
                    "Unknown fraud command type: "
                            + envelope.getCommandType()
            );
        }

        fraudCheckService.check(
                envelope.getCommandId(),
                objectMapper.treeToValue(
                        envelope.getPayload(),
                        FraudCheckRequestedCommand.class
                )
        );
    }
}
