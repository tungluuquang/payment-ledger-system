package org.vippro.account_service.messaging.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.vippro.account_service.service.AccountCommandService;
import org.vippro.command.AccountDebitRequestedCommand;
import org.vippro.command.AccountCreditRequestedCommand;
import org.vippro.command.ReverseAccountDebitCommand;
import org.vippro.command.ReverseAccountCreditCommand;
import org.vippro.messaging.CommandEnvelope;

@Component
@RequiredArgsConstructor
public class AccountCommandConsumer {

    private final ObjectMapper objectMapper;
    private final AccountCommandService accountCommandService;

    @KafkaListener(
            topics = "account-commands",
            containerFactory = "accountCommandKafkaListenerContainerFactory"
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

        switch (envelope.getCommandType()) {
            case "AccountDebitRequestedCommand" -> {
                AccountDebitRequestedCommand command =
                        objectMapper.treeToValue(
                                envelope.getPayload(),
                                AccountDebitRequestedCommand.class
                        );
                accountCommandService.debit(envelope.getCommandId(), command);
            }
            case "ReverseAccountDebitCommand" -> {
                ReverseAccountDebitCommand command =
                        objectMapper.treeToValue(
                                envelope.getPayload(),
                                ReverseAccountDebitCommand.class
                        );
                accountCommandService.reverse(envelope.getCommandId(), command);
            }
            case "AccountCreditRequestedCommand" -> {
                AccountCreditRequestedCommand command =
                        objectMapper.treeToValue(
                                envelope.getPayload(),
                                AccountCreditRequestedCommand.class
                        );
                accountCommandService.credit(envelope.getCommandId(), command);
            }
            case "ReverseAccountCreditCommand" -> {
                ReverseAccountCreditCommand command =
                        objectMapper.treeToValue(
                                envelope.getPayload(),
                                ReverseAccountCreditCommand.class
                        );
                accountCommandService.reverseCredit(
                        envelope.getCommandId(),
                        command
                );
            }
            default -> throw new IllegalArgumentException(
                    "Unknown account command type: "
                            + envelope.getCommandType()
            );
        }
    }
}
