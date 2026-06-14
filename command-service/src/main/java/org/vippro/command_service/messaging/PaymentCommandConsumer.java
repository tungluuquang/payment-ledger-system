package org.vippro.command_service.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.vippro.command.CancelPaymentCommand;
import org.vippro.command.CompletePaymentCommand;
import org.vippro.command.InitiatePaymentCommand;
import org.vippro.command_service.service.PaymentCommandService;
import org.vippro.messaging.CommandEnvelope;

@Component
@RequiredArgsConstructor
public class PaymentCommandConsumer {

    private final ObjectMapper objectMapper;
    private final PaymentCommandService paymentCommandService;

    @KafkaListener(
            topics = "payment-commands",
            containerFactory = "paymentCommandKafkaListenerContainerFactory"
    )
    public void consume(String message) throws JsonProcessingException {
        CommandEnvelope envelope = objectMapper.readValue(
                message,
                CommandEnvelope.class
        );
        validateEnvelope(envelope);

        switch (envelope.getCommandType()) {
            case "InitiatePaymentCommand" ->
                    paymentCommandService.initiate(
                            envelope.getCommandId(),
                            objectMapper.treeToValue(
                                    envelope.getPayload(),
                                    InitiatePaymentCommand.class
                            )
                    );
            case "CompletePaymentCommand" ->
                    paymentCommandService.complete(
                            envelope.getCommandId(),
                            objectMapper.treeToValue(
                                    envelope.getPayload(),
                                    CompletePaymentCommand.class
                            )
                    );
            case "CancelPaymentCommand" ->
                    paymentCommandService.cancel(
                            envelope.getCommandId(),
                            objectMapper.treeToValue(
                                    envelope.getPayload(),
                                    CancelPaymentCommand.class
                            )
                    );
            default -> throw new IllegalArgumentException(
                    "Unknown payment command type: "
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
